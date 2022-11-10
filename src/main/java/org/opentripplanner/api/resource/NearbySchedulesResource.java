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
package org.opentripplanner.api.resource;

import static org.opentripplanner.api.resource.ServerInfo.Q;

import java.util.*;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.xml.bind.annotation.XmlRootElement;

import org.apache.commons.lang.StringUtils;
import org.locationtech.jts.geom.Coordinate;
import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.gtfs.model.Stop;
import org.opentripplanner.api.util.StopFinder;
import org.opentripplanner.common.model.T2;
import org.opentripplanner.index.model.*;
import org.opentripplanner.model.StopPattern;
import org.opentripplanner.routing.alertpatch.AlertPatch;
import org.opentripplanner.routing.algorithm.AStar;
import org.opentripplanner.routing.core.RouteMatcher;
import org.opentripplanner.routing.core.RoutingRequest;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.core.TraverseMode;
import org.opentripplanner.routing.core.TraverseModeSet;
import org.opentripplanner.routing.edgetype.TripPattern;
import org.opentripplanner.routing.error.VertexNotFoundException;
import org.opentripplanner.routing.graph.GraphIndex;
import org.opentripplanner.routing.graph.LIRRSolariDataService;
import org.opentripplanner.routing.trippattern.TripTimes;
import org.opentripplanner.routing.vertextype.TransitStop;
import org.opentripplanner.standalone.OTPServer;
import org.opentripplanner.standalone.Router;
import org.opentripplanner.util.DateUtils;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Lookup arrival/departure times for a group of stops, by location of interest or list of stops.
 */
@Path("/routers/{routerId}/nearby")
@XmlRootElement
public class NearbySchedulesResource {

    /**
     * latitude of origin of search. Either origin, list of stops, or both must be supplied.
     */
    @QueryParam("lat")
    public Double lat;

    /**
     * longitude of origin of search.
     */
    @QueryParam("lon")
    public Double lon;

    /**
     * Maximum walking distance, in meters, that the search will use to find stops.
     */
    @QueryParam("radius")
    public Double radius;

    /**
     * list of stops of interest. Should be a comma-separated list in the format MTA:101001,MNR:1, etc. Ignored
     * if lat, lon, and radius are given; required otherwise.
     */
    @QueryParam("stops")
    public String stopsStr;

    /**
     * maximum number of stops to return if lat, lon, and radius are given; Ignored if stops are given;
     */
    @QueryParam("maxStops")
    @DefaultValue("100")
    public Integer maxStops;

    /**
     * Minimum number of stops to return if lat, lon, and radius are given. Will search past radius to find stops.
     */
    @QueryParam("minStops")
    @DefaultValue("1")
    public Integer minStops;

    /**
     * Timeout for graph search, in seconds. In the future, this value may be overridden by a system maximum.
     */
    @QueryParam("timeout")
    @DefaultValue("1.0")
    public Double timeout;

    /**
     * list of routes of interest. Should be in the format MTASBWY__A,MNR__1, etc. Optional.
     */
    @QueryParam("routes")
    public String routesStr;

    /**
     * direction of interest. Optional. Use GTFS direction_id (1 or 0).
     */
    @QueryParam("direction")
    public Integer direction;

    /**
     * date to return arrival/departure times for. Will default to the current date.
     */
    @QueryParam("date")
    public String date;

    /**
     * time to return arrival/departure times for. Will default to the current time.
     */
    @QueryParam("time")
    public String time;

    /**
     * Range, in seconds, from given time, in which to return arrival/departure results.
     */
    @QueryParam("timeRange")
    @DefaultValue("1800")
    public int timeRange;

    /**
     * Maximum number of departures to return per TripPattern, per stop
     */
    @QueryParam("numberOfDepartures")
    @DefaultValue("3")
    public int numberOfDepartures;

    /**
     * If true, omit non-pickups, i.e. arrival/departures where the vehicle does not pick up passengers
     */
    @QueryParam("omitNonPickups")
    @DefaultValue("true")
    public boolean omitNonPickups;

    /**
     * if given, tripHeadsign to return arrival/departure time for.
     */
    @QueryParam("tripHeadsign")
    public String tripHeadsign;

    /**
     * if given, only include trips that visit this stop
     */
    @QueryParam("stoppingAt")
    public String stoppingAt;

    /**
     * If true, group arrivals/departures by parent stop (station), instead of by stop.
     */
    @QueryParam("groupByParent")
    @DefaultValue("true")
    public boolean groupByParent;

    /**
     * List of agencies that are excluded from the stopTime results
     */
    @QueryParam("bannedAgencies")
    public String bannedAgencies;

    /**
     * List of route types that are excluded from the stopTime results
     */
    @QueryParam("bannedRouteTypes")
    public String bannedRouteTypes;

    /** The set of modes that a user is willing to use, with qualifiers stating whether vehicles should be parked, rented, etc.
     * Allowable values (order of modes in set is not significant):
     * <table class="table">
     *     <tr><th>mode</th><th>Parameter value</th></tr>
     *     <tr><td>Walk only</td><td>WALK</td></tr>
     *     <tr><td>Drive only</td><td>CAR</td></tr>
     *     <tr><td>Bicycle only</td><td>BICYCLE</td></tr>
     *     <tr><td>Transit</td><td>TRANSIT,WALK</td></tr>
     *     <tr><td>Park-and-ride</td><td>CAR_PARK,TRANSIT</td></tr>
     *     <tr><td>Kiss-and-ride</td><td>CAR,TRANSIT</td></tr>
     *     <tr><td>Bicycle and transit</td><td>BICYCLE,TRANSIT</td></tr>
     *     <tr><td>Bicycle and ride</td><td>BICYCLE_PARK,TRANSIT</td></tr>
     *     <tr><td>Bikeshare</td><td>BICYCLE_RENT</td></tr>
     *     <tr><td>Bikeshare and transit</td><td>BICYCLE_RENT,TRANSIT</td></tr>
     * </table>
     *
     * In addition, restrict transit usage to a mode by replacing TRANSIT with any subset of the following:
     * SUBWAY, RAIL, BUS, FERRY, CABLE_CAR, GONDOLA, FUNICULAR, AIRPLANE
     */
    @QueryParam("mode")
    public String mode;

    /**
     * Include cancelled trips in the output
     */
    @QueryParam("showCancelledTrips")
    @DefaultValue("false")
    public boolean showCancelledTrips;

    /**
     * Add all stops for a given trip to the output
     */
    @QueryParam("includeStopsForTrip")
    @DefaultValue("false")
    public boolean includeStopsForTrip;

    /**
     * A list of tracks for which to display arrivals, e.g. "1" or "1,2". Default to all tracks.
     */
    @QueryParam("tracks")
    public String trackIds = null;

    /**
     * Sets API to a mode for digital signage features. This removes results that are not in realtime.
     */
    @QueryParam("signMode")
    @DefaultValue("true")
    public boolean signMode;

    private GraphIndex index;

    private LIRRSolariDataService lirrSolari;

    private Router router;
    
    public NearbySchedulesResource(@Context OTPServer otpServer, @PathParam("routerId") String routerId) {
        Router router = otpServer.getRouter(routerId);
        this.router = router;
        index = router.graph.index;
        lirrSolari = index.lirrSolari;
    }

    public NearbySchedulesResource(Router router) {
        this.router = router;
        index = router.graph.index;
        lirrSolari = index.lirrSolari;
    }

    /**
     * Return upcoming vehicle arrival/departure times at given stops. Matches stops by lat/lon/radius,
     * and/or by list of stops. Arrival/departure times can be filtered by route and direction.
     * @return . .
     */
    @GET
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML + Q})
    public Collection<StopTimesByStop> getNearbySchedules() throws Exception {
        boolean isLatLonSearch = lat != null && lon != null && radius != null;
        long startTime = getStartTimeSec();

        Collection<TransitStop> transitStops;
        Map<TransitStop, State> transitStopStates = null;
        if (isLatLonSearch) {
            transitStopStates = getNearbyStops(lat, lon, radius);
            transitStops = transitStopStates.keySet();
        } else if (stopsStr != null) {
            transitStops = getStopsFromList(stopsStr);
        } else {
            throw new IllegalArgumentException("Must supply lat/lon/radius, or list of stops.");
        }
        
        Map<AgencyAndId, StopTimesByStop> stopIdAndStopTimesMap =  getStopTimesByParentStop(transitStops, startTime,
        		transitStopStates);
        Collection<StopTimesByStop> stopTimesByStops = stopIdAndStopTimesMap.values();
        for (StopTimesByStop stbs : stopTimesByStops) {
            stbs.limitTimes(startTime, timeRange, numberOfDepartures);
        }

        return stopIdAndStopTimesMap.values();
    }

    private Map<AgencyAndId, StopTimesByStop> getStopTimesByParentStop(Collection<TransitStop> transitStops, 
    		long startTime, Map<TransitStop, State> stateMap) throws Exception {
        Map<AgencyAndId, StopTimesByStop> stopIdAndStopTimesMap = new LinkedHashMap<>();
        List<AlertPatch> alertPatchSnapshot = index.graph.getAlertPatchesAsList();    	

        RouteMatcher routeMatcher = RouteMatcher.parse(routesStr);
        for (TransitStop tstop : transitStops) {
            if(tstop != null) {
                Stop stop = tstop.getStop();
                AgencyAndId key = key(stop);

                Set<TraverseMode> modes = getModes();
                Set<String> bannedAgencies = getBannedAgencies();
                Set<Integer> bannedRouteTypes = getBannedRouteTypes();

                /* filter by mode */
                if(modes != null && !stopHasMode(tstop, modes)){
                    continue;
                }

                if (router.defaultRoutingRequest.bannedStopsNearby.matches(stop)) {
                    continue;
                }
                
                if(stop.getLocationType() != Stop.LOCATION_TYPE_STATION && stop.getLocationType() != Stop.LOCATION_TYPE_STOP)
                	continue;

                Stop requiredStop = null;
                if (stoppingAt != null){
                    AgencyAndId id = AgencyAndId.convertFromString(stoppingAt, ':');
                    requiredStop = index.stopForId.get(id);
                }

                // schedule/RT arrivals
                StopTimesForPatternsQuery scheduleRtQuery = new StopTimesForPatternsQuery
                                                        .Builder(stop, startTime, timeRange, numberOfDepartures, omitNonPickups)
                                                        .routeMatcher(routeMatcher)
                                                        .direction(direction)
                                                        .tripHeadsign(tripHeadsign)
                                                        .requiredStop(requiredStop)
                                                        .bannedAgencies(bannedAgencies)
                                                        .bannedRouteTypes(bannedRouteTypes)
                                                        .trackIds(getTrackIds())
                                                        .showCancelledTrips(showCancelledTrips)
                                                        .includeStopsForTrip(includeStopsForTrip)
                                                        .includeTripPatterns(true)
                                                        .signMode(signMode)
                                                        .build();

                List<StopTimesInPattern> stopTimesPerPattern = index.stopTimesForStop(scheduleRtQuery);

                // arrivals from Solari
            	List<Entry<T2<String, String>, JsonNode>> solariMessages = this.lirrSolari.solariDataByTripAndStop.entrySet()
                		.stream()
                		.filter(it -> { return it.getKey().second.equals(AgencyAndId.convertToString(stop.getId())); })
                		.collect(Collectors.toList());

                StopTimesByStop stopTimes = stopIdAndStopTimesMap.get(key);

                if (stopTimes == null) {
                    if (stateMap != null) {
                        State state = stateMap.get(tstop);
                        double distance = state.getWalkDistance();
                        LinkedList<Coordinate> coords = new LinkedList<>();
                        for (State s = state; s != null; s = s.getBackState()) {
                            coords.addFirst(s.getVertex().getCoordinate());
                        }
                        long time = state.getElapsedTimeSeconds();
                        stopTimes = new StopTimesByStop(stop, distance, time, coords, groupByParent);
                    } else {
                        stopTimes = new StopTimesByStop(stop, groupByParent);
                    }
                    
                    stopIdAndStopTimesMap.put(key, stopTimes);
                }
                
            	// if solari has trips for the station we're looking for, use those--otherwise use the RT/schedule 

            	// use Solari preferentially
                if(!solariMessages.isEmpty()) {
                	stopTimesPerPattern = new ArrayList<StopTimesInPattern>();

                    for(Entry<T2<String, String>, JsonNode> e : solariMessages) {
                		JsonNode solariPacket = e.getValue();
                		String tripGtfsId = e.getKey().first;

                		// the case where Solari has trips that are not in the schedule or in GTFS RT, so 
        	        	// we need to pluck what we can from its packet and add it to the data structures here
                		if(tripGtfsId.startsWith("LI_TRAIN_NO_")) { // this string means the trip is fake
                			stopTimes.addPatternsViaSolariPacket(solariPacket, index);
                		} else {
                	        Date date = new Date(startTime * 1000);
        	        		TripPattern p = index.getTripPatternForTripId(AgencyAndId.convertFromString(tripGtfsId));

        	        		// TODO: what a mess... 
        	        		// NOTE: filters disabled so we don't filter trips Solari is showing
                            StopTimesForPatternQuery solariQuery = new StopTimesForPatternQuery.Builder(p, date,
                                    router.graph.timetableSnapshotSource.getTimetableSnapshot(), stop).build();

        	        		StopTimesInPattern stopTimesForPattern = index.getStopTimesForPattern(solariQuery);
                			
        	        		if(stopTimesForPattern != null)
        	        			stopTimes.addPatternWithSolariPacket(stopTimesForPattern, solariPacket, index);
        	        		else
                    			stopTimes.addPatternsViaSolariPacket(solariPacket, index);
                		}
                	}

                // otherwise use patterns from GTFS/GTFS-RT
                } else {
                    stopTimes.addPatterns(stopTimesPerPattern);                
                }

                if (includeStopsForTrip) {
                    for (StopTimesByRouteAndHeadsign str : stopTimes.getGroups()) {
                        Set<TripTimeShort> times = str.getTimes();
                        for (TripTimeShort t : times) {
                            Iterator<StopShort> stopsIter = t.stopsForTrip.iterator();
                            int stopIndex = 0;
                            while (stopsIter.hasNext()) {
                                StopShort tt = stopsIter.next();
                                if (!tt.id.equals(stop.getId())) {
                                    TripPattern p = index.getTripPatternForTripId(t.tripId);
                                    if (p.stopPattern.dropoffs[stopIndex] == StopPattern.PICKDROP_NONE) {
                                        stopsIter.remove();
                                    }
                                }
                                stopIndex++;
                            }
                        }
                    }
                }
                	
                addAlertsToStopTimes(stop, stopTimes, alertPatchSnapshot);
            }
        }


        return stopIdAndStopTimesMap;
    }

    private void addAlertsToStopTimes(Stop stop, StopTimesByStop stopTimes, List<AlertPatch> agencyPatchSnapshot){
        Collection<TripPattern> tripPatterns = null;
        tripPatterns = index.patternsForStop.get(stop);

        if(stop.getId() == null)
        	return;

        /*
         * DEBUG TOOL 
         * 
        for(AlertPatch a : agencyPatchSnapshot) {
	    	Trip t = router.graph.index.tripForId.get(a.getTrip());
	    	TripPattern p = router.graph.index.patternForTrip.get(t);
	    	Timetable tt = router.graph.index.currentUpdatedTimetableForTripPattern(p);
	    	TripTimes ttimes = tt.getTripTimes(t);
	    	    	
	    	long start = new ServiceDate().getAsDate().getTime() 
	    			+ (ttimes.getDepartureTime(0) * 1000);
	
	    	long end = new ServiceDate().getAsDate().getTime() 
	    			+ (ttimes.getDepartureTime(ttimes.getNumStops() - 1) * 1000);
	
	    	System.out.println("Trip=" + t + " start=" + new DateTime(start) + " end=" + new DateTime(end));
	    	
	    	if(new DateTime().toDate().getTime() > start && 
	    		new DateTime().toDate().getTime() < end) {
	    		System.out.println("Trip=" + t + " lastStop=" + p.getStops().get(p.getStops().size() -1));
	    	}
        }
        */
    	    	
        
        if (tripPatterns != null) {
            for (TripPattern tripPattern : tripPatterns) {
                if (direction != null && !(direction + "").equals(tripPattern.getDirection())) {
                    continue;
                }

                // trip patches--patches are filtered for a trip match elsewhere
                List<AlertPatch> vehiclePosUpdates = agencyPatchSnapshot.stream()
                		.filter(it -> it.hasVehicleInfo() && it.getTrip() != null)
                		.collect(Collectors.toList());

                for(AlertPatch a : vehiclePosUpdates) {
                	stopTimes.addAlert(a, new Locale("en"));
                }
                	
                for (int i = 0; i < tripPattern.stopPattern.stops.length; i++) {
                    if (stop == null || stop.equals(tripPattern.stopPattern.stops[i])) {
                        AlertPatch[] alertPatchesBoardEdges = index.graph.getAlertPatches(tripPattern.boardEdges[i]);
                        AlertPatch[] alertPatchesAlightEdges = index.graph.getAlertPatches(tripPattern.alightEdges[i]);

                        for(AlertPatch alertPatch : alertPatchesBoardEdges){
                            stopTimes.addAlert(alertPatch, new Locale("en"));
                        }
                        for(AlertPatch alertPatch : alertPatchesAlightEdges){
                            stopTimes.addAlert(alertPatch, new Locale("en"));
                        }
                    }
                }
            }
        }
    }

    private AgencyAndId key(Stop stop) {
        if (stop.getParentStation() == null || !groupByParent) {
            return stop.getId();
        }
        else {
            return new AgencyAndId(stop.getId().getAgencyId(), stop.getParentStation());
        }
    }

    private long getStartTimeSec() {
        if (time != null && date != null) {
            Date d = DateUtils.toDate(date, time, index.graph.getTimeZone());
            if (d == null) {
                throw new IllegalArgumentException("badly formatted time and date");
            }
            return d.getTime() / 1000;
        }
        // this calculation is done by index.stopTimesForStop if time=0, but do it here so we can reuse value
        return System.currentTimeMillis() / 1000;
    }

    private Map<TransitStop, State> getNearbyStops(double lat, double lon, double radius) {
    	try {
	        RoutingRequest options = router.defaultRoutingRequest.clone();
	        options.modes = new TraverseModeSet(TraverseMode.WALK);
	        options.batch = true;
	        options.setFrom(lat, lon);
	        options.setRoutingContext(index.graph);
	        AStar search = new AStar();
	        StopFinder finder = new StopFinder(radius, minStops, maxStops, groupByParent, getModes());
	        search.setTraverseVisitor(finder);
	        search.getShortestPathTree(options, -1, finder);
	        return finder.getStops();
    	} catch (VertexNotFoundException e) {
    		return new HashMap<TransitStop, State>();
    	}
    }

    private List<TransitStop> getStopsFromList(String stopsStr) {
        List<Stop> stops = new ArrayList<>();
        for (String st : stopsStr.split(",")) {
            AgencyAndId id = AgencyAndId.convertFromString(st, ':');
            Stop stop = index.stopForId.get(id);
            if (stop == null) {
                // first try interpreting stop as a parent
                Collection<Stop> children = index.stopsForParentStation.get(id);
                if (children.isEmpty()) {
                    stops.add(null);
                }
                stops.addAll(children);
            } else {
                stops.add(stop);
            }
        }
        return stops.stream().map(index.stopVertexForStop::get)
                .collect(Collectors.toList());
    }

    private boolean stopHasMode(TransitStop tstop, Set<TraverseMode> modes){
        for (TraverseMode mode : modes) {
            if (tstop.getModes().contains(mode)) {
                return true;
            }
        }
        return false;
    }

    private Set<String> getBannedAgencies() {
        if (StringUtils.isNotBlank(bannedAgencies))
            return new HashSet<String>(Arrays.asList(bannedAgencies.split(",")));
        return null;
    }

    private Set<Integer> getBannedRouteTypes() {
        if (StringUtils.isNotBlank(bannedRouteTypes)) {
            HashSet<Integer> bannedRouteTypesSet = new HashSet<>();
            for (String bannedRouteType : bannedRouteTypes.split(",")) {
                bannedRouteTypesSet.add(Integer.parseInt(bannedRouteType));
            }
            return bannedRouteTypesSet;
        }
        return null;
    }

    private Set<TraverseMode> getModes(){
        if(StringUtils.isNotBlank(mode)) {
            HashSet<TraverseMode> modes = new HashSet<>();
            String[] elements = mode.split(",");
            if (elements != null) {
                for (int i = 0; i < elements.length; i++) {
                    TraverseMode mode = TraverseMode.valueOf(elements[i].trim());
                    if (mode != null) {
                        modes.add(mode);
                    }
                }
                return modes;
            }
        }
        return null;
    }

    private Collection<String> getTrackIds() {
        return trackIds == null ? null : Arrays.asList(trackIds.split(","));
    }
}