/* This program is free software: you can redistribute it and/or
 modify it under the terms of the GNU Lesser General Public License
 as published by the Free Software Foundation, either version 3 of
 the License, or (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>. */

package org.opentripplanner.routing.impl;

import com.csvreader.CsvReader;
import org.onebusaway.gtfs.model.Agency;
import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.gtfs.model.Route;
import org.onebusaway.gtfs.model.Trip;
import org.opentripplanner.routing.core.Fare;
import org.opentripplanner.routing.core.Fare.FareType;
import org.opentripplanner.routing.core.FareBundle;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.core.WrappedCurrency;
import org.opentripplanner.routing.edgetype.DwellEdge;
import org.opentripplanner.routing.edgetype.HopEdge;
import org.opentripplanner.routing.edgetype.StreetEdge;
import org.opentripplanner.routing.graph.Edge;
import org.opentripplanner.routing.services.FareService;
import org.opentripplanner.routing.spt.GraphPath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.charset.Charset;
import java.util.*;
import java.util.stream.Collectors;

/** NYC fare condition types */
enum NycFareConditionType {
    peak_hour_only,
    non_peak_hour_only
}

/** NYC transfer agreement types */
enum NycTransferType {
    free,
    free_step_up,
    merge //Used for Zones fares with transfers
}

/** A service is a combination of agency and route type */
class NycServiceId implements Serializable {
    String agencyId;
    int routeType;

    NycServiceId(String agencyId, int routeType) {
        this.agencyId = agencyId;
        this.routeType = routeType;
    }

    public String getAgencyId() {
        return this.agencyId;
    }
    public int getRouteType() {
        return this.routeType;
    }

    public String toString() {
        return convertToString(this);
    }

    public static String convertToString(NycServiceId sid) {
        return sid == null ? null : sid.getAgencyId() + '_' + sid.getRouteType();
    }
}

/** Holds service basic fare info */
class NycAgencyFare implements Serializable {
    NycServiceId serviceId;
    FareType fareType;
    NycFareConditionType fareConditionType;
    float price;
    String startZone;
    String endZone;
    String midZone;
    long startTime;
    long endTime;

    NycAgencyFare(NycServiceId serviceId,
                  FareType fareType,
                  NycFareConditionType fareConditionType,
                  float price,
                  String startZone,
                  String endZone,
                  String midZone) {

        this(serviceId, fareType, fareConditionType, price, startZone, endZone, midZone, 0, 0);

    }

    NycAgencyFare(NycServiceId serviceId,
                  FareType fareType,
                  NycFareConditionType fareConditionType,
                  float price,
                  String startZone,
                  String endZone,
                  String midZone,
                  long startTime,
                  long endTime
                  ) {
        this.serviceId = serviceId;
        this.fareType = fareType;
        this.fareConditionType = fareConditionType;
        this.price = price;
        this.startZone = startZone;
        this.endZone = endZone;
        this.midZone = midZone;
        this.startTime = startTime;
        this.endTime = endTime;
    }

    public String getKey() {
        String internalKey = this.serviceId.toString() + '_' + this.fareType.toString();
        if(this.fareConditionType != null) {
            internalKey += '_' + this.fareConditionType.toString();
        }
        if(this.startZone != null && !this.startZone.isEmpty()) {
            internalKey += "_" + this.startZone;
        }
        if(this.midZone != null && !this.midZone.isEmpty()) {
            internalKey += "_" + this.midZone;
        }
        if(this.endZone != null && !this.endZone.isEmpty()) {
            internalKey += "_" + this.endZone;
        }
        if (this.startTime != 0) {
            internalKey += ":" + this.startTime;
        }
        if (this.endTime != 0) {
            internalKey += ":" + this.endTime;
        }


        return internalKey;
    }
}

/** Holds agency peak hours */
class NycAgencyPeakHour implements Serializable {
    NycServiceId serviceId;
    Integer[] days; // most likely weekday
    Integer[] hours;
    boolean useGtfs;

    NycAgencyPeakHour(NycServiceId serviceId, Integer[] days, Integer[] hours, boolean useGtfs) {
        this.serviceId = serviceId;
        this.days = days;
        this.hours = hours;
        this.useGtfs = useGtfs;
    }

    public String getKey() {
        return this.serviceId.toString();
    }
}

/** Holds transfer agreement rules between a pair of services */
class NycTransferRule implements Serializable {
    NycServiceId serviceId;
    NycServiceId otherServiceId;
    NycTransferType transferType;
    int transferDuration;

    NycTransferRule(NycServiceId serviceId, NycServiceId otherServiceId, NycTransferType transferType, int transferDuration) {
        this.serviceId = serviceId;
        this.otherServiceId = otherServiceId;
        this.transferType = transferType;
        this.transferDuration = transferDuration;
    }

    public String getKey() {
        return this.serviceId.toString() + '_' + this.otherServiceId.toString();
    }
}

/** Internal class to hold previous traveled services */
class NycTraveledService implements Serializable {
    NycServiceId serviceId;
    AgencyAndId agencyAndId;
    long rideTime;
    float price;
    String startZone;
    String midZone;

    NycTraveledService(NycServiceId serviceId, AgencyAndId agencyAndId, long rideTime, float price) {
        this.serviceId = serviceId;
        this.agencyAndId = agencyAndId;
        this.rideTime = rideTime;
        this.price = price;
    }

    public String getKey() {
        return this.agencyAndId.toString() + '_' + this.rideTime;
    }
}

/**
 * This handles the New York City advanced fare rules:
 * 1. fixed fare
 * 2. zone-to-zone fare
 * 3. service-to-service transfer rules
 * 4. peak hour rules
 */
public class NycAdvancedFareServiceImpl implements FareService, Serializable {
    private static final Logger LOG = LoggerFactory.getLogger(NycFareServiceImpl.class);

    private static final long serialVersionUID = 1L;
    HashMap<String,NycServiceId> agencies = new HashMap<String, NycServiceId>();
    HashMap<String, NycAgencyFare> agencyFares = new HashMap<String, NycAgencyFare>();
    HashMap<String, NycTransferRule> transferRules = new HashMap<String, NycTransferRule>();
    HashMap<String, NycAgencyPeakHour> agencyPeakHours = new HashMap<String, NycAgencyPeakHour>();

    private String fareDirectory = null;

    public NycAdvancedFareServiceImpl(String fareDirectory) {

        this.fareDirectory = fareDirectory;

        ////////////////////////////////////////////////////////
        // Create Agencies
        ///////////////////////////////////////////////////////
        loadAgencies(fareDirectory + "/agencies-fares.csv");

        ////////////////////////////////////////////////////////
        // Create Agency Fares and Peak Hours
        ///////////////////////////////////////////////////////
        Set<String> agencyKeys = agencies.keySet()
                .stream()
                .collect(Collectors.toSet());

        loadFares(fareDirectory+"/fares.csv");

        for ( String a : agencyKeys ) {
            NycServiceId tempServiceId = agencies.get(a);
            NycAgencyPeakHour peakHours = new NycAgencyPeakHour(tempServiceId, null, null, true);
            agencyPeakHours.put(peakHours.getKey(), peakHours);
        }

        ////////////////////////////////////////////////////////
        // TRANSFER RULES
        ///////////////////////////////////////////////////////
        loadTransferRules(fareDirectory+"/transfer_rules-fares.csv");

    }

    private void loadAgencies(String csvFileName) {
        InputStream is = null;
        try {
            is = new FileInputStream(csvFileName);
        } catch (FileNotFoundException e) {
            LOG.error("expected file not found csvFileName=" + csvFileName);
            LOG.error("agencies will not be generated!");
            return;
        }
        CsvReader reader = new CsvReader(is, ',', Charset.forName("UTF-8"));
        try {
            reader.readHeaders();
            while (reader.readRecord()) {

                String serviceIdStr = reader.get("service_id");
                String agencyId = reader.get("agency_id");
                int routeType = Integer.parseInt(reader.get("route_type"));

                NycServiceId serviceId = new NycServiceId(agencyId, routeType);

                agencies.put(serviceIdStr, serviceId);
            }
        } catch (IOException ex) {
            LOG.error("Exception while loading agencies for fares CSV.");
        }
    }

    private void loadTransferRules(String csvFileName) {
        InputStream is = null;
        try {
            is = new FileInputStream(csvFileName);
        } catch (FileNotFoundException e) {
            LOG.error("expected file not found csvFileName=" + csvFileName);
            LOG.error("Transfers will not be generated!");
            return;
        }
        CsvReader reader = new CsvReader(is, ',', Charset.forName("UTF-8"));
        try {
            reader.readHeaders();
            while (reader.readRecord()) {

                String transferTypeStr = reader.get("transfer_type");
                NycTransferType transferType;

                String serviceIdStr = reader.get("service_id");
                String otherServiceIdStr = reader.get("other_service_id");

                if ("merge".equals(transferTypeStr)) {
                    transferType = NycTransferType.merge;
                } else if ("free_step_up".equals(transferTypeStr)) {
                    transferType = NycTransferType.free_step_up;
                } else {
                    transferType = NycTransferType.free;
                }

                int transferDuration = Integer.parseInt(reader.get("transfer_duration"));

                NycTransferRule rule = new NycTransferRule(agencies.get(serviceIdStr),agencies.get(otherServiceIdStr), transferType,transferDuration);

                transferRules.put(rule.getKey(), rule);
            }
        } catch (IOException ex) {
            LOG.error("Exception while loading fare transfer rules CSV.");
        }

    }

    private void loadFares(String csvFileName) {
        InputStream is = null;
        try {
            is = new FileInputStream(csvFileName);
        } catch (FileNotFoundException e) {
            LOG.error("expected file not found csvFileName=" + csvFileName);
            LOG.error("fares will not be generated!");
            return;
        }
        CsvReader reader = new CsvReader(is, ',', Charset.forName("UTF-8"));
        try {
            reader.readHeaders();
            while (reader.readRecord()) {

                NycServiceId fareServiceId = agencies.get(reader.get("service_id"));

                FareType fareType;
                NycFareConditionType fareConditionType;
                String fareTypeStr = reader.get("fare_type");
                String fareConditionTypeStr = reader.get("fare_condition_type");

                if ("regular".equals(fareTypeStr)) {
                    fareType = FareType.regular;
                } else if ("student".equals(fareTypeStr)) {
                    fareType = FareType.student;
                } else if ("senior".equals(fareTypeStr)) {
                    fareType = FareType.senior;
                } else if ("tram".equals(fareTypeStr)) {
                    fareType = FareType.tram;
                } else if ("special".equals(fareTypeStr)) {
                    fareType = FareType.special;
                } else if ("youth".equals(fareTypeStr)) {
                    fareType = FareType.youth;
                } else {
                    fareType = FareType.regular;
                }

                if ("peak".equals(fareConditionTypeStr)) {
                    fareConditionType = NycFareConditionType.peak_hour_only;
                } else if ("nonpeak".equals(fareConditionTypeStr)) {
                    fareConditionType = NycFareConditionType.non_peak_hour_only;
                } else {
                    fareConditionType = null;
                }

                float price = Float.parseFloat(reader.get("price"));

                String startZone;
                String midZone;
                String endZone;

                if (reader.get("start_zone").length() > 0) {
                    startZone = reader.get("start_zone");
                } else {
                    startZone = null;
                }
                if (reader.get("mid_zone").length() > 0) {
                    midZone = reader.get("mid_zone");
                } else {
                    midZone = null;
                }
                if (reader.get("end_zone").length() > 0) {
                    endZone = reader.get("end_zone");
                } else {
                    endZone = null;
                }

                long startTime;
                long endTime;

                if (reader.get("start_time").length() > 0) {
                    startTime = Long.parseLong(reader.get("start_time"));
                } else {
                    startTime = 0;
                }
                if (reader.get("end_time").length() > 0) {
                    endTime = Long.parseLong(reader.get("end_time"));
                } else {
                    endTime = 0;
                }

                NycAgencyFare fare = new NycAgencyFare(fareServiceId, fareType, fareConditionType, price, startZone, endZone,
                        midZone, startTime, endTime);

                agencyFares.put(fare.getKey(), fare);
            }
        } catch (IOException ex) {
            LOG.error("Exception while loading fare table CSV.");
        }
    }

    @Override
    public Fare getCost(GraphPath path) {
        FareBundle fareBundle = getLegCostBreakDown(path);

        if(fareBundle != null) {
            return fareBundle.fare;
        } else {
            return null;
        }
    }

    @Override
    public FareBundle getLegCostBreakDown(GraphPath path) {
        LinkedList<State> states = path.states;
        final int WALK = -1;
        final int SUBWAY = 1;

        // create rides
        List<Ride> rides = new ArrayList<Ride>();
        Ride newRide = null;
        RideChain newChain = new RideChain();
        newChain.isPeak = false;

        for (State state : states) {
            Edge backEdge = state.getBackEdge();
            if (backEdge instanceof StreetEdge) {
                if (newRide == null || !newRide.classifier.equals(WALK)) {
                    if (rides.size() == 0 || !rides.get(rides.size() - 1).classifier.equals(WALK)) {
                        newRide = new Ride();
                        newRide.classifier = WALK;
                        newRide.chain = newChain;
                        rides.add(newRide);
                    }
                }
                continue;
            }

            // dwells do not affect fare.
            if (backEdge instanceof DwellEdge)
                continue;

            if (!(backEdge instanceof HopEdge)) {
                newRide = null;
                continue;
            }
            AgencyAndId routeId = state.getRoute();
            Agency agency = state.getBackTrip().getRoute().getAgency();
            String agencyId = agency.getId();
            if (routeId == null) {
                newRide = null;
            } else {
                if (newRide == null || !routeId.equals(newRide.route)) {
                    newRide = new Ride();
                    newRide.agency = agency.getId();
                    newRide.chain = newChain;
                    rides.add(newRide);

                    newRide.firstStop = ((HopEdge) backEdge).getBeginStop();
                    newRide.startZone = newRide.firstStop.getZoneId();
                    newRide.route = routeId;
                    Trip trip = state.getBackTrip();
                    newRide.gtfsTrip = trip;
                    Route route = trip.getRoute();
                    int type = route.getType();
                    newRide.routeType = type;
                    NycServiceId serviceId = new NycServiceId(agencyId, type);
                    newRide.classifier = serviceId;
                    newRide.startTime = state.getTimeSeconds();
                }
                newRide.lastStop = ((HopEdge) backEdge).getEndStop();
                newRide.endZone = newRide.lastStop.getZoneId();


            }
        }

        // There are no rides, so there's no fare.
        if (rides.size() == 0) {
            return null;
        }

        Currency currency = Currency.getInstance("USD");
        Fare totalFare = new Fare();
        Map<String, Fare> legFares = new HashMap<String, Fare>();
        for (FareType fareType : FareType.values()) {
            // Holds previously traveled services in the same ride
            Set<NycTraveledService> traveledServices = new HashSet<NycTraveledService>();

            float totalPrice = 0.0f;
            HashSet<AgencyAndId> unknownFareRoutes = new HashSet<AgencyAndId>(); //TODO: how to include it into the response
            int prevRouteType = WALK;


            //iterate rides to calculate total fare
            boolean hasFare = false; // whether fare exists
            for (Ride ride : rides) {
                // walk is free
                if (ride.classifier.equals(WALK)) {
                    prevRouteType = WALK;
                    continue;
                }

                // if in-station subway transfer, then free
                if (ride.routeType == prevRouteType && ride.routeType == SUBWAY) {
                    prevRouteType = ride.routeType;
                    continue;
                }

                long currentRideTime = ride.startTime;

                // find agency fare settings
                NycAgencyFare agencyFare = findAgencyFare(ride, fareType);

                // log it if fare unknown
                if (agencyFare == null) {
                    unknownFareRoutes.add(ride.route);
                    prevRouteType = ride.routeType;
                    continue;
                }

                hasFare = true;

                // get basic fare without transfer
                float basicFare = agencyFare.price;

                // start calculating transfer fare
                boolean transferFound = false;
                boolean mergeZone = false;
                float transferFare = basicFare;

                NycTraveledService targetPrevTraveledService = null; // the target that applies transfer rule into current ride
                for (NycTraveledService traveledService : traveledServices) {
                    String prevServiceIdString = traveledService.serviceId.toString();
                    String thisServiceIdString = ride.agency + "_" + ride.routeType;

                    Set<NycTransferRule> rules = getRelatedTransferRules(prevServiceIdString, thisServiceIdString);
                    if (rules.isEmpty()) {
                        continue;
                    }

                    // calculate transfer fare
                    long prevServiceRideTime = traveledService.rideTime;
                    for (NycTransferRule tRule : rules) {
                        if (tRule.transferDuration > 0) {
                            // first check transfer duration
                            if ((currentRideTime - prevServiceRideTime > tRule.transferDuration)) {
                                continue;
                            }
                        }

                        // loop transfer type
                        switch (tRule.transferType) {
                            case free:
                                transferFound = true;
                                transferFare = 0.0f;
                                break;
                            case free_step_up:
                                transferFound = true;
                                if (traveledService.price >= transferFare) {
                                    transferFare = 0.0f;
                                } else {
                                    transferFare -= traveledService.price;
                                }
                                break;
                            case merge:
                                // If we found a merge zone transfer, then set the traveled Service origin for future reference.
                                Ride lastRide = null;

                                int index = rides.indexOf(ride);
                                if(index > 0)
                                  lastRide = rides.get(index-1);

                                if(traveledService.startZone == null){
                                    traveledService.startZone = lastRide.startZone;
                                    traveledService.midZone = lastRide.endZone;
                                }
                                ride.mergeStartZone = traveledService.startZone;

                                //Look for indirect route patterns
                                if(lastRide != null) {
                                    if ((isLessThan(lastRide.endZone, traveledService.midZone) && isLessThan(traveledService.midZone, ride.mergeStartZone)) ||
                                            (isGreaterThan(lastRide.endZone, traveledService.midZone) && isGreaterThan(traveledService.midZone, ride.mergeStartZone))) {
                                        traveledService.midZone = lastRide.endZone;
                                    }
                                }
                                ride.mergeMidZone = traveledService.midZone;

                                agencyFare = findAgencyFare(ride, fareType);
                                if(agencyFare == null) {
                                	totalPrice -= traveledService.price;
                                	break;
                                }
                                transferFare = agencyFare.price - traveledService.price;
                                traveledService.price = agencyFare.price;
                                transferFound = true;
                                mergeZone = true;
                                break;

                            default:
                                break;

                        }

                        // transfer found
                        if (transferFound) {
                            break;
                        }
                    }

                    if (transferFound) {
                        targetPrevTraveledService = traveledService;
                        break;
                    }
                }

                float legPrice = 0;
                if (!transferFound) {
                    legPrice = basicFare;
                    // no transfer rules from previous services, add basic fare to total
                    totalPrice += legPrice;
                    if (legPrice > 0) {
                        // log paid service so future rides can check transfer rules
                        NycTraveledService traveledService = new NycTraveledService(new NycServiceId(ride.agency, ride.routeType), ride.route, currentRideTime, basicFare);
                        traveledServices.add(traveledService);
                    }
                } else {
                    legPrice = transferFare;
                    // transfer rule found, add transfer fare to total
                    totalPrice += legPrice;

                    // One transfer is allowed in one trip (unless using merge zone rules)
                    // remove the service that covers the transfer to current ride
                    if (targetPrevTraveledService != null && !mergeZone) {
                        traveledServices.remove(targetPrevTraveledService);
                    }
                }

                String routeId = ride.route.toString();
                if(!legFares.containsKey(routeId)) {
                    legFares.put(routeId, new Fare());
                }

                Fare legFare = legFares.get(routeId);
                legFare.addFare(fareType, new WrappedCurrency(currency),
                        (int) Math.round(legPrice
                                * Math.pow(10, currency.getDefaultFractionDigits())));

                prevRouteType = ride.routeType;
            }

            if(hasFare) {
                totalFare.addFare(fareType, new WrappedCurrency(currency),
                        (int) Math.round(totalPrice
                                * Math.pow(10, currency.getDefaultFractionDigits())));
            }
        }

        return new FareBundle(totalFare, legFares);
    }


    /** find agency fare for the ride based on requested fare type */
    private NycAgencyFare findAgencyFare(Ride ride, FareType fareType) {
        String serviceIdString = ride.agency + "_" + ride.routeType;
        boolean isPeak = isInPeakHour(ride);

        // zone-pair key component (used in finding specific fare rule)
        String zoneKey = "";
        if(ride.mergeStartZone != null && !ride.mergeStartZone.isEmpty()) {
            zoneKey += '_' + ride.mergeStartZone;
        }
        else if(ride.startZone != null && !ride.startZone.isEmpty()) {
            zoneKey += '_' + ride.startZone;
        }
        if(ride.mergeMidZone != null && !ride.mergeMidZone.isEmpty()
                && (
                        (isLessThan(ride.mergeMidZone,ride.mergeStartZone) && isLessThan(ride.mergeMidZone, ride.endZone))
                    || (isGreaterThan(ride.mergeMidZone,ride.mergeStartZone) && isGreaterThan(ride.mergeMidZone, ride.endZone))
                    || ride.mergeMidZone.equals(ride.endZone))){
            zoneKey += '_' + ride.mergeMidZone;
        }
        if(ride.endZone != null && !ride.endZone.isEmpty()) {
            zoneKey += '_' + ride.endZone;
        }

        String baseFareKey = serviceIdString + '_' + fareType.toString();


        // check if there's fare without conditions
        String nonConditionFareKey = baseFareKey;
        if(!zoneKey.isEmpty()) {
            nonConditionFareKey += zoneKey;
        }

        nonConditionFareKey = getFareKeyForTime(nonConditionFareKey, ride.startTime);
        NycAgencyFare nonConditionFare = agencyFares.get(nonConditionFareKey);

        // check if there's fare with conditions
        NycAgencyFare conditionFare = null;
        for (NycFareConditionType fareConditionType : NycFareConditionType.values()) {
            String fareKey = baseFareKey + "_" + fareConditionType.toString();
            fareKey = getFareKeyForTime(fareKey, ride.startTime);
            // first check fixed fare (without startZone & endZone)
            conditionFare = agencyFares.get(fareKey);
            if(conditionFare == null) {
                fareKey = baseFareKey + "_" + fareConditionType.toString() + zoneKey;
                fareKey = getFareKeyForTime(fareKey, ride.startTime);
                conditionFare = agencyFares.get(fareKey);
            }

            if(conditionFare == null) {
                continue;
            }

            // check if meet fare conditions
            switch (conditionFare.fareConditionType) {
                case peak_hour_only :
                    if(!isPeak) {
                        conditionFare = null; // reset if not meet
                    }
                    break;
                case non_peak_hour_only :
                    if(isPeak) {
                        conditionFare = null; // reset if not meet
                    }

                    break;
                default:
                    break;
            }

            if(conditionFare != null) {
                break;
            }
        }

        if(conditionFare != null) {
            return conditionFare;
        } else {
            return nonConditionFare;
        }
    }

    /** check if the ride is in Peak Hour */
    private boolean isInPeakHour(Ride ride) {
        if(ride.chain.isPeak){
            return true;
        }
        String ruleKey = ride.agency + "_" + ride.routeType;
        NycAgencyPeakHour peakHours = agencyPeakHours.get(ruleKey);

        // no peak hour rules for this service
        if (peakHours == null) {
            return false;
        }

        // if GTFS defines peak or off peak
        if(peakHours.useGtfs) {
            if (ride.gtfsTrip.getPeakOffpeak() == 1) {
                ride.chain.isPeak = true;
                return true;
            }
            else
                return false;
        }

        // if we manually check the hours
        long rideTimeSeconds = ride.startTime;
        Date rideTime = new Date(rideTimeSeconds * 1000);
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(rideTime);
        Integer dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK);
        Integer hour = calendar.get(Calendar.HOUR_OF_DAY);

        //List<int> days = Arrays.asList(peakHours.days);
        if(Arrays.asList(peakHours.days).contains(dayOfWeek) && Arrays.asList(peakHours.hours).contains(hour)) {
            ride.chain.isPeak = true;
            return true;
        }
        else
            return false;
    }


    /** check to see if zone A is less than zone B */
    private boolean isLessThan(String zoneA, String zoneB){
    	if(zoneA == null || zoneB == null)
    		return false;
        return Integer.parseInt(zoneA) <= Integer.parseInt(zoneB);
    }

    /** check to see if zone A is less than zone B */
    private boolean isGreaterThan(String zoneA, String zoneB){
    	if(zoneA == null || zoneB == null)
    		return false;
        return Integer.parseInt(zoneA) >= Integer.parseInt(zoneB);
    }

    /** query transfer rules related to a specific service */
    private Set<NycTransferRule> getRelatedTransferRules(String serviceIdString, String otherServiceIdString) {
        return transferRules.entrySet().stream()
                .filter(
                        r -> (
                                r.getValue().serviceId.toString().equals(serviceIdString) &&
                                        r.getValue().otherServiceId.toString().equals(otherServiceIdString)
                        )
                )
                .map(p -> p.getValue())
                .collect(Collectors.toSet());
    }

    /** query for fare key that matches a given datetime */
    private String getFareKeyForTime(String key, long time) {
        String fareKey = key; // default to key for all time

        Set<String> fareKeys = agencyFares.keySet()
                .stream()
                .filter(s -> (s.startsWith(key) && s.contains(":")))
                .collect(Collectors.toSet());

        for ( String k : fareKeys ) {

            NycAgencyFare tempFare = agencyFares.get(k);

            if ((time >= tempFare.startTime) && (time <= tempFare.endTime)) {
                fareKey = k; // returns first key that matches the time constraint
                break;
            }
        }

        return fareKey;
    }
}
