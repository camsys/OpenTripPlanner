package org.opentripplanner.api.resource;

import org.opentripplanner.api.model.ApiItinerary;
import org.opentripplanner.api.model.ApiLeg;
import org.opentripplanner.api.model.ApiTripPlan;
import org.opentripplanner.api.model.ApiTripSearchMetadata;
import org.opentripplanner.api.model.error.PlannerError;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.UriInfo;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;

/** Represents a trip planner response, will be serialized into XML or JSON by Jersey */
public class TripPlannerResponse {

    private static final Logger LOG = LoggerFactory.getLogger(TripPlannerResponse.class);

    /** A dictionary of the parameters provided in the request that triggered this response. */
    public HashMap<String, String> requestParameters;
    private ApiTripPlan plan;
    private ApiTripSearchMetadata metadata;
    private PlannerError error = null;

    /** Debugging and profiling information */
    public DebugOutput debugOutput = null;

    public ElevationMetadata elevationMetadata = null;

    /** This no-arg constructor exists to make JAX-RS happy. */ 
    @SuppressWarnings("unused")
    private TripPlannerResponse() {};

    /** Construct an new response initialized with all the incoming query parameters. */
    public TripPlannerResponse(UriInfo info) {
        this.requestParameters = new HashMap<String, String>();
        if (info == null) { 
            // in tests where there is no HTTP request, just leave the map empty
            return;
        }
        for (Entry<String, List<String>> e : info.getQueryParameters().entrySet()) {
            // include only the first instance of each query parameter
            requestParameters.put(e.getKey(), e.getValue().get(0));
        }
    }

    // NOTE: the order the getter methods below is semi-important, in that Jersey will use the
    // same order for the elements in the JS or XML serialized response. The traditional order
    // is request params, followed by plan, followed by errors.

    /** The actual trip plan. */
    public ApiTripPlan getPlan() {
        return plan;
    }

    public void setPlan(ApiTripPlan plan) {
        this.plan = plan;
    }

    public ApiTripSearchMetadata getMetadata() {
        return metadata;
    }

    public void setMetadata(ApiTripSearchMetadata metadata) {
        this.metadata = metadata;
    }

    /** The error (if any) that this response raised. */
    public PlannerError getError() {
        return error;
    }

    public void setError(PlannerError error) {
        this.error = error;
    }

    public void setBookingUrlParams() throws IOException, InterruptedException {
        for (ApiItinerary i : plan.itineraries) {
            for (ApiLeg leg : i.legs) {
                if (leg.dropOffBookingInfo == null || leg.agencyId == null) {
                    continue;
                }
                List<String> validAgencyNames = new ArrayList<>(Arrays.asList("ZIPS","OTTER","ROLLING","ROCHESTER"));
                boolean hasProperAgency = false;
                for (String validAgencyName : validAgencyNames) {
                    if (leg.agencyId.toUpperCase().contains(validAgencyName) || (leg.agencyName != null && leg.agencyName.toUpperCase().contains(validAgencyName))) {
                        hasProperAgency = true;
                    }
                }
                if (!hasProperAgency) {
                    continue;
                }

                ArrayList<String> addressList = new ArrayList<>(Arrays.asList(leg.from.name.split(",")));
                if (addressList.size() < 5) {
                    String zip = reverseGeocodeZip(leg.from.lon.toString() + "%2C" + leg.from.lat.toString());
                    if (zip == null) {
                        LOG.error("address: " + leg.from.name + "  was parsed incorrectly");
                        return;
                    }
                    addressList.add(addressList.size() - 1, zip);
                } else if (addressList.size() > 5) {
                    LOG.warn("Possible malformed address " + leg.from.name + " detected; truncating");
                    String addressStart = String.join(",", addressList.subList(0,addressList.size()-4));
                    addressList = new ArrayList<>(addressList.subList(addressList.size()-5,addressList.size()));
                    addressList.remove(0);
                    addressList.add(0,addressStart);
                }
                String pickupAddressStreetAddress = addressList.get(0).strip();
                String pickupAddressLocation = addressList.get(1).strip() + "," + addressList.get(2);
                String pickupAddressPostalCode = addressList.get(3).strip();
                String pickupAddressLatLon = leg.from.lat.toString() + "," + leg.from.lon.toString();
                String pickupDateTime = Instant.ofEpochMilli(leg.from.departure.getTime().getTime()).atZone(ZoneId.of("-5")).minusHours(1).format(DateTimeFormatter.RFC_1123_DATE_TIME);

                addressList = new ArrayList<>(Arrays.asList(leg.to.name.split(",")));
                if (addressList.size() < 5) {
                    String zip = reverseGeocodeZip(leg.to.lon.toString() + "%2C" + leg.to.lat.toString());
                    if (zip == null) {
                        LOG.error("address: " + leg.to.name + "  was parsed incorrectly");
                        return;
                    }
                    addressList.add(addressList.size() - 1, zip);
                } else if (addressList.size() > 5) {
                    LOG.warn("Possible malformed address " + leg.to.name + " detected; truncating");
                    String addressStart = String.join(",", addressList.subList(0,addressList.size()-4));
                    addressList = new ArrayList<>(addressList.subList(addressList.size()-5,addressList.size()));
                    addressList.remove(0);
                    addressList.add(0,addressStart);
                }

                String dropoffAddressStreetAddress = addressList.get(0).strip();
                String dropoffAddressLocation = addressList.get(1).strip() + "," + addressList.get(2);
                String dropoffAddressPostalCode = addressList.get(3).strip();
                String dropoffAddressLatLon = leg.to.lat.toString() + "," + leg.to.lon.toString();
                String dropoffDateTime = Instant.ofEpochMilli(leg.to.arrival.getTime().getTime()).atZone(ZoneId.of("-5")).minusHours(1).format(DateTimeFormatter.RFC_1123_DATE_TIME);

                String agencyId = leg.agencyId.replaceAll("[^\\d.]", ""); // remove all non-integers

                String agencyContact = leg.dropOffBookingInfo.getContactInfo().getPhoneNumber().replace("(","").replace(")","");
                String agencyName = leg.agencyName;
                String arriveBy = requestParameters.get("arriveBy") != null ? requestParameters.get("arriveBy") : "false";

                StringBuilder sb = new StringBuilder();
                sb.append("https://booking.qa.mndot.camsys-apps.com/booking-portal?");
                sb.append("pickupAddressStreetAddress=").append(pickupAddressStreetAddress);
                sb.append("&");
                sb.append("pickupAddressLocation=").append(pickupAddressLocation);
                sb.append("&");
                sb.append("pickupAddressPostalCode=").append(pickupAddressPostalCode);
                sb.append("&");
                sb.append("pickupAddressLatLon=").append(pickupAddressLatLon);
                sb.append("&");
                sb.append("pickupDateTime=").append(pickupDateTime);
                sb.append("&");
                sb.append("dropoffAddressStreetAddress=").append(dropoffAddressStreetAddress);
                sb.append("&");
                sb.append("dropoffAddressLocation=").append(dropoffAddressLocation);
                sb.append("&");
                sb.append("dropoffAddressPostalCode=").append(dropoffAddressPostalCode);
                sb.append("&");
                sb.append("dropoffAddressLatLon=").append(dropoffAddressLatLon);
                sb.append("&");
                sb.append("dropoffDateTime=").append(dropoffDateTime);
                sb.append("&");
                sb.append("agencyId=").append(agencyId);
                sb.append("&");
                sb.append("agencyContact=").append(agencyContact);
                sb.append("&");
                sb.append("agencyName=").append(agencyName);
                sb.append("&");
                sb.append("arriveBy=").append(arriveBy);

                leg.bookingUrl = sb.toString();
            }
        }
    }

    private String reverseGeocodeZip(String latlon) throws IOException, InterruptedException {
        HttpClient client = HttpClient.newBuilder().build();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://geocode.arcgis.com/arcgis/rest/services/World/GeocodeServer/reverseGeocode?location=" + latlon + "&maxLocations=1&f=json&outFields=postal&featureTypes=postal"))
                .header("Content-Type", "application/json")
                .GET()
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            return "";
        }
        String postal = response.body().substring(22,27);
        return postal;
    }
}