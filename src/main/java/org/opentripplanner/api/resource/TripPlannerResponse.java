package org.opentripplanner.api.resource;

import org.opentripplanner.api.model.ApiItinerary;
import org.opentripplanner.api.model.ApiLeg;
import org.opentripplanner.api.model.ApiTripPlan;
import org.opentripplanner.api.model.ApiTripSearchMetadata;
import org.opentripplanner.api.model.error.PlannerError;
import org.opentripplanner.ext.flex.FlexTripsMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.UriInfo;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;
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

    public void setBookingUrlParams() {
        for (ApiItinerary i : plan.itineraries) {
            for (ApiLeg leg : i.legs) {
                String[] addressArray = leg.from.name.split(",");
                if (addressArray.length < 5) {
                    LOG.error("address: " + leg.from.name + "  was parsed incorrectly");
                    return;
                }
                String pickupAddressStreetAddress = addressArray[0].strip();
                String pickupAddressLocation = addressArray[1].strip() + "," + addressArray[2];
                String pickupAddressPostalCode = addressArray[3].strip();
                String pickupAddressLatLon = leg.from.lat.toString() + "," + leg.from.lon.toString();
                String pickupDateTime = Instant.ofEpochMilli(leg.from.departure.getTime().getTime()).atZone(ZoneId.of("-5")).minusHours(1).format(DateTimeFormatter.RFC_1123_DATE_TIME);

                addressArray = leg.to.name.split(",");
                if (addressArray.length < 5) {
                    LOG.error("address: " + leg.to.name + "  was parsed incorrectly");
                    return;
                }

                String dropoffAddressStreetAddress = addressArray[0].strip();
                String dropoffAddressLocation = addressArray[1].strip() + "," + addressArray[2];
                String dropoffAddressPostalCode = addressArray[3].strip();
                String dropoffAddressLatLon = leg.to.lat.toString() + "," + leg.to.lon.toString();
                String dropoffDateTime = Instant.ofEpochMilli(leg.to.arrival.getTime().getTime()).atZone(ZoneId.of("-5")).minusHours(1).format(DateTimeFormatter.RFC_1123_DATE_TIME);

                String agencyId = leg.agencyId.replaceAll("[^\\d.]", ""); // remove all non-integers

                String agencyContact = leg.dropOffBookingInfo.getContactInfo().getPhoneNumber().replace("(","").replace(")","");
                String agencyName = leg.agencyName;

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

                leg.bookingUrl = sb.toString();
            }
        }
    }
}