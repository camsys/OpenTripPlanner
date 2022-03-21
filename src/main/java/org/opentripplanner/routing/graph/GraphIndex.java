package org.opentripplanner.routing.graph;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.TimeUnit;

import javax.ws.rs.core.Response;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import org.apache.commons.math3.util.Pair;
import org.apache.lucene.util.PriorityQueue;
import org.joda.time.LocalDate;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.onebusaway.gtfs.model.Agency;
import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.gtfs.model.FeedInfo;
import org.onebusaway.gtfs.model.Route;
import org.onebusaway.gtfs.model.Stop;
import org.onebusaway.gtfs.model.StopTime;
import org.onebusaway.gtfs.model.Trip;
import org.onebusaway.gtfs.model.calendar.ServiceDate;
import org.onebusaway.gtfs.services.calendar.CalendarService;
import org.opentripplanner.common.LuceneIndex;
import org.opentripplanner.common.geometry.HashGridSpatialIndex;
import org.opentripplanner.common.geometry.SphericalDistanceLibrary;
import org.opentripplanner.common.model.GenericLocation;
import org.opentripplanner.common.model.P2;
import org.opentripplanner.gtfs.GtfsLibrary;
import org.opentripplanner.index.IndexGraphQLSchema;
import org.opentripplanner.index.model.StopTimesInPattern;
import org.opentripplanner.index.model.TripTimeShort;
import org.opentripplanner.model.StopPattern;
import org.opentripplanner.profile.ProfileTransfer;
import org.opentripplanner.profile.StopCluster;
import org.opentripplanner.profile.StopNameNormalizer;
import org.opentripplanner.profile.StopTreeCache;
import org.opentripplanner.routing.algorithm.AStar;
import org.opentripplanner.routing.algorithm.TraverseVisitor;
import org.opentripplanner.routing.core.RouteMatcher;
import org.opentripplanner.routing.core.RoutingRequest;
import org.opentripplanner.routing.core.ServiceDay;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.core.TraverseMode;
import org.opentripplanner.routing.edgetype.PathwayEdge;
import org.opentripplanner.routing.edgetype.TablePatternEdge;
import org.opentripplanner.routing.edgetype.Timetable;
import org.opentripplanner.routing.edgetype.TimetableSnapshot;
import org.opentripplanner.routing.edgetype.TripPattern;
import org.opentripplanner.routing.spt.DominanceFunction;
import org.opentripplanner.routing.trippattern.FrequencyEntry;
import org.opentripplanner.routing.trippattern.TripTimes;
import org.opentripplanner.routing.vertextype.TransitStationStop;
import org.opentripplanner.routing.vertextype.TransitStop;
import org.opentripplanner.standalone.OTPMain;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;

import graphql.ExecutionResult;
import graphql.GraphQL;

/**
 * This class contains all the transient indexes of graph elements -- those that are not
 * serialized with the graph. Caching these maps is essentially an optimization, but a big one.
 * The index is bootstrapped from the graph's list of edges.
 */
public class GraphIndex {
	
    private static final Logger LOG = LoggerFactory.getLogger(GraphIndex.class);
    private static final int CLUSTER_RADIUS = 500; // meters

    /** maximum distance to walk after leaving transit in Analyst */
    public static final int MAX_WALK_METERS = 3500;

    // TODO: consistently key on model object or id string
    public final Map<String, Vertex> vertexForId = Maps.newHashMap();
    public final Map<String, Map<String, Agency>> agenciesForFeedId = Maps.newHashMap();
    public final Map<String, FeedInfo> feedInfoForId = Maps.newHashMap();
    public final Map<AgencyAndId, Stop> stopForId = Maps.newHashMap();
    public final Map<AgencyAndId, Trip> tripForId = Maps.newHashMap();
    public final Map<AgencyAndId, Route> routeForId = Maps.newHashMap();
    public final Map<AgencyAndId, String> serviceForId = Maps.newHashMap();
    public final Map<String, TripPattern> patternForId = Maps.newHashMap();
    public final Map<Stop, TransitStop> stopVertexForStop = Maps.newHashMap();
    public final Map<Trip, TripPattern> patternForTrip = Maps.newHashMap();
    public final Multimap<String, TripPattern> patternsForFeedId = ArrayListMultimap.create();
    public final Multimap<Route, TripPattern> patternsForRoute = ArrayListMultimap.create();
    public final Multimap<Stop, TripPattern> patternsForStop = ArrayListMultimap.create();
    public final Multimap<AgencyAndId, Stop> stopsForParentStation = ArrayListMultimap.create();
    final HashGridSpatialIndex<TransitStop> stopSpatialIndex = new HashGridSpatialIndex<TransitStop>();
    public final Map<Stop, StopCluster> stopClusterForStop = Maps.newHashMap();
    public final Map<String, StopCluster> stopClusterForId = Maps.newHashMap();
    public final Multimap<String, PathwayEdge> pathwayForElevator = ArrayListMultimap.create();
    public final Map<AgencyAndId, HashSet<PathwayEdge>> equipmentEdgesForStationId = new HashMap<AgencyAndId, HashSet<PathwayEdge>>();
	public final Map<String, HashSet<Vertex>> connectionsFromMap = new HashMap<String, HashSet<Vertex>>();
    public Cache<String, Set<Route>> routesForMtaComplexCache = CacheBuilder.newBuilder().expireAfterWrite(5 , TimeUnit.MINUTES ).build();
    public Set<String> signModeHideSchedule = new HashSet<>();

	public RemoteCSVBackedHashMap mtaSubwayStations = null;

	public RemoteCSVBackedHashMap mtaSubwayComplexes = null;

	public LIRRSolariDataService lirrSolari = null;
	
    /* Should eventually be replaced with new serviceId indexes. */
    private final CalendarService calendarService;
    private final Map<AgencyAndId,Integer> serviceCodes;

    /* Full-text search extensions */
    public LuceneIndex luceneIndex;

    /* Separate transfers for profile routing */
    public Multimap<StopCluster, ProfileTransfer> transfersFromStopCluster;
    private HashGridSpatialIndex<StopCluster> stopClusterSpatialIndex = null;

    /* This is a workaround, and should probably eventually be removed. */
    public Graph graph;

    /** Used for finding first/last trip of the day. This is the time at which service ends for the day. */
    public final int overnightBreak = 60 * 60 * 2; // FIXME not being set, this was done in transitIndex

    public GraphQL graphQL;

    /** Store distances from each stop to all nearby street intersections. Useful in speeding up analyst requests. */
    private transient StopTreeCache stopTreeCache = null;

    /** Keep a time value for testing */
    private long time = -1;

    public GraphIndex (Graph graph) {
    	LOG.info("Indexing graph...");
        
        for (String feedId : graph.getFeedIds()) {
            for (Agency agency : graph.getAgencies(feedId)) {
                Map<String, Agency> agencyForId = agenciesForFeedId.getOrDefault(feedId, new HashMap<>());
                agencyForId.put(agency.getId(), agency);
                this.agenciesForFeedId.put(feedId, agencyForId);
            }
            this.feedInfoForId.put(feedId, graph.getFeedInfo(feedId));
        }

        Collection<Edge> edges = graph.getEdges();

        /* We will keep a separate set of all vertices in case some have the same label. 
         * Maybe we should just guarantee unique labels. */
        Set<Vertex> vertices = Sets.newHashSet();
        for (Edge edge : edges) {
            vertices.add(edge.getFromVertex());
            vertices.add(edge.getToVertex());
            if (edge instanceof TablePatternEdge) {
                TablePatternEdge patternEdge = (TablePatternEdge) edge;
                TripPattern pattern = patternEdge.getPattern();
                patternForId.put(pattern.code, pattern);
            }
            if (edge instanceof PathwayEdge) {
                PathwayEdge pathwayEdge = (PathwayEdge) edge;
                if (pathwayEdge.isElevator()) {
                	pathwayForElevator.put(pathwayEdge.getElevatorId(), pathwayEdge);
                }
            }
        }
        for (Vertex vertex : vertices) {
            vertexForId.put(vertex.getLabel(), vertex);
            if (vertex instanceof TransitStop) {
                TransitStop transitStop = (TransitStop) vertex;
                Stop stop = transitStop.getStop();
                stopForId.put(stop.getId(), stop);
                stopVertexForStop.put(stop, transitStop);
                AgencyAndId parent = new AgencyAndId(stop.getId().getAgencyId(), stop.getParentStation());
                stopsForParentStation.put(parent, stop);
            }
        }
        
        for (TransitStop stopVertex : stopVertexForStop.values()) {
            Envelope envelope = new Envelope(stopVertex.getCoordinate());
            
            if(stopVertex.getLat() == StopTime.MISSING_VALUE && stopVertex.getLon() == StopTime.MISSING_VALUE)
            	continue;
            
            stopSpatialIndex.insert(envelope, stopVertex);
        }
        for (TripPattern pattern : patternForId.values()) {
            patternsForFeedId.put(pattern.getFeedId(), pattern);
            patternsForRoute.put(pattern.route, pattern);

            for (Trip trip : pattern.getTrips()) {
                patternForTrip.put(trip, pattern);
                tripForId.put(trip.getId(), trip);
            }
            for (Stop stop: pattern.getStops()) {
                patternsForStop.put(stop, pattern);
            }
        }
        for (Route route : patternsForRoute.asMap().keySet()) {
            routeForId.put(route.getId(), route);
        }

        // Copy these two service indexes from the graph until we have better ones.
        calendarService = graph.getCalendarService();
        serviceCodes = graph.serviceCodes;
        this.graph = graph;
        graphQL = GraphQL.newGraphQL(new IndexGraphQLSchema(this).indexSchema).build();
/*        
        graphQL = new GraphQL(
                new IndexGraphQLSchema(this).indexSchema,
                new ExecutorServiceExecutionStrategy(Executors.newCachedThreadPool(
                        new ThreadFactoryBuilder().setNameFormat("GraphQLExecutor-" + graph.routerId + "-%d").build()
                )));
*/
        LOG.info("Done indexing graph.");
        

        // go through all vertices and for each entrance, walk all the outgoing edges--
        // if there's at least one accessible path to a platform (defined by a location type that isn't 
        // connected via a pathway), mark this entrance "accessible". 
        //
        // Also, only change falses to true--if a prior process (e.g. GTFS) has marked a stop accessible, don't
        // second guess that assessment
        //
        LOG.info("Computing pathway/station accessibility via pathways...");

        for (Vertex v : vertices) {
        	if(!(v instanceof TransitStop) && !(v instanceof TransitStationStop))
        		continue;

//        	System.out.println("Starting pathway walk from " + v.getLabel());

        	TransitStationStop tss = (TransitStationStop)v;
        	
        	if(v.getLabel() == null || v.getLabel().isEmpty())
        		continue;
        	
        	Stop thisStop = this.stopForId.get(GtfsLibrary.convertIdFromString(v.getLabel()));

        	if(thisStop == null)
          		continue;
  
          	AgencyAndId parentStopId = (thisStop.getParentStation() != null) 
        			? new AgencyAndId(thisStop.getId().getAgencyId(), thisStop.getParentStation()) 
        			: thisStop.getId();

        	HashSet<PathwayEdge> equipmentAccessFromHere = equipmentEdgesForStationId.get(parentStopId);
        	if(equipmentAccessFromHere == null)
        		equipmentAccessFromHere = new HashSet<PathwayEdge>();

        	HashSet<Vertex> connectionsFromHere = new HashSet<Vertex>();
        	HashSet<Vertex> visitedList = new HashSet<Vertex>();
        	boolean initialState = graph.stopAccessibilityStrategy.transitStopEvaluateGTFSAccessibilityFlag(tss.getStop());
        	
        	Boolean hasAtLeastOneAccessiblePath = walkPathwayEdges(v, connectionsFromHere, equipmentAccessFromHere, visitedList, initialState, 0);       
        	if(hasAtLeastOneAccessiblePath != null && v instanceof TransitStop) {
            	TransitStop ts = (TransitStop)v;
        		ts.setWheelchairEntrance(hasAtLeastOneAccessiblePath);
        	}
        	
        	// change from the two AgencyAndId formats used in this codebase (sigh...)
        	connectionsFromMap.put(v.getLabel().replace(":", "_"), connectionsFromHere);

        	// update equipment index
        	if(thisStop != null)
	        	equipmentEdgesForStationId.put(parentStopId, equipmentAccessFromHere);
        }
        
        LOG.info("done");
        
        // MTA specific services only used at runtime, not during bundle building
    	if(System.getProperty("isBundleBuilder") == null || !System.getProperty("isBundleBuilder").equals("true")) { 	
	    	try {
	    		mtaSubwayStations = new RemoteCSVBackedHashMap("http://web.mta.info/developers/data/nyct/subway/Stations.csv", "MTASBWY");
	    		mtaSubwayComplexes = new RemoteCSVBackedHashMap("http://web.mta.info/developers/data/nyct/subway/StationComplexes.csv", "MTASBWY");
	    		lirrSolari = new LIRRSolariDataService(graph);
	    	} catch(Exception e) {
	    		e.printStackTrace();
	    	}
    	}

    	JsonNode routerConfig = getRouterConfig();
    	if(routerConfig != null){
            JsonNode signModeHideScheduleConfig = routerConfig.get("signModeHideSchedule");
            if (signModeHideScheduleConfig != null) {
                signModeHideSchedule = new HashSet<>(Arrays.asList(signModeHideScheduleConfig.asText().split(",")));
            }
        }
    }

    private JsonNode getRouterConfig(){
        if(graph != null && graph.routerConfig != null){
            ObjectMapper objectMapper = new ObjectMapper();
            try {
                JsonNode routerConfig = objectMapper.readValue(graph.routerConfig, JsonNode.class);
                return routerConfig;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

	private Boolean walkPathwayEdges(Vertex v, HashSet<Vertex> connectionsFromHere, HashSet<PathwayEdge> equipmentAccessFromHere, 
			HashSet<Vertex> visitedList, Boolean accessibleToHere, int depth) {    	

		// stop if we've been here before
        if(visitedList.contains(v))
        	return accessibleToHere;
        visitedList.add(v);
        
		int newDepth = depth + 1;
    	
		TransitStationStop tss = (TransitStationStop)v;
        Stop s = tss.getStop();

//    	System.out.println(StringUtils.repeat(">", newDepth) + " Vertex:" + v.getLabel() + " accessibleToHere=" + accessibleToHere);
    	
        if(s.getLocationType() == Stop.LOCATION_TYPE_ENTRANCE_EXIT || s.getLocationType() == Stop.LOCATION_TYPE_STOP)
        	connectionsFromHere.add(v); 

        // don't evaluate bus stops or stations with no entrances
        if(v instanceof TransitStop && !((TransitStop) v).hasEntrances())        
        	return accessibleToHere;
        
        // if there are no pathways to evaluate, stop here
        if(v.getOutgoing().stream().filter(PathwayEdge.class::isInstance).count() == 0)
        	return accessibleToHere;
        	        
    	// don't go out of the station--terminate search
        if(s.getLocationType() == Stop.LOCATION_TYPE_ENTRANCE_EXIT)
        	return accessibleToHere;

    	Boolean hasAtLeastOneAccessiblePath = false;
    	for(Edge e : v.getOutgoing()) {
        	// only follow pathways
    		if(!(e instanceof PathwayEdge))
    			continue;

        	PathwayEdge pe = (PathwayEdge)e;   
//        	System.out.println(StringUtils.repeat(">", newDepth) + " Pathway Edge:" + pe.getPathwayId() + " accessible=" + e.isWheelchairAccessible() + " mode=" + pe.getPathwayMode());

    		boolean newAccessibleToHere = accessibleToHere;

    		if(pe.getElevatorId() != null) {
    			equipmentAccessFromHere.add(pe);
    		}
    		
    		// "generic nodes" in GTFS don't have lat/longs, so use the station's coordinate
    		// to display the node on a map. 
    		Coordinate toLocation = e.getToVertex().getCoordinate();
    		if(toLocation.x == StopTime.MISSING_VALUE || toLocation.y == StopTime.MISSING_VALUE)
    			toLocation = v.getCoordinate();    		
    		pe.setGeometry(v.getCoordinate(), toLocation);
    		
    		// false is sticky--that is, once we get one false, the whole result is false
    		if(newAccessibleToHere == true)
    			newAccessibleToHere = e.isWheelchairAccessible();
    		
    		// true is sticky here
    		if(walkPathwayEdges(e.getToVertex(), connectionsFromHere, equipmentAccessFromHere, visitedList, newAccessibleToHere, newDepth) 
    			&& !hasAtLeastOneAccessiblePath)
    			hasAtLeastOneAccessiblePath = true;
    	}

    	return hasAtLeastOneAccessiblePath;
    }

    
    /**
     * Stop clustering is slow to perform and only used in profile routing for the moment.
     * Therefore it is not done automatically, and any method requiring stop clusters should call this method
     * to ensure that the necessary indexes are lazy-initialized.
     */
    public synchronized void clusterStopsAsNeeded() {
        if (stopClusterSpatialIndex == null) {
            clusterStops();
            LOG.info("Creating a spatial index for stop clusters.");
            stopClusterSpatialIndex = new HashGridSpatialIndex<StopCluster>();
            for (StopCluster cluster : stopClusterForId.values()) {
                Envelope envelope = new Envelope(new Coordinate(cluster.lon, cluster.lat));
                stopClusterSpatialIndex.insert(envelope, cluster);
            }
        }
    }

    private void analyzeServices() {
        // This is a mess because CalendarService, CalendarServiceData, etc. are all in OBA.
        // TODO catalog days of the week and exceptions for each service day.
        // Make a table of which services are running on each calendar day.
        // Really the calendarService should be entirely replaced with a set
        // of simple indexes in GraphIndex.
    }

    /** Get all trip patterns running through any stop in the given stop cluster. */
    private Set<TripPattern> patternsForStopCluster(StopCluster sc) {
        Set<TripPattern> tripPatterns = Sets.newHashSet();
        for (Stop stop : sc.children) tripPatterns.addAll(patternsForStop.get(stop));
        return tripPatterns;
    }

    /**
     * Initialize transfer data needed for profile routing.
     * Find the best transfers between each pair of patterns that pass near one another.
     */
    public void initializeProfileTransfers() {
        transfersFromStopCluster = HashMultimap.create();
        final double TRANSFER_RADIUS = 500.0; // meters
        Map<P2<TripPattern>, ProfileTransfer.GoodTransferList> transfers = Maps.newHashMap();
        LOG.info("Finding transfers between clusters...");
        for (StopCluster sc0 : stopClusterForId.values()) {
            Set<TripPattern> tripPatterns0 = patternsForStopCluster(sc0);
            // Accounts for area-like (rather than point-like) nature of clusters
            Map<StopCluster, Double> nearbyStopClusters = findNearbyStopClusters(sc0, TRANSFER_RADIUS);
            for (StopCluster sc1 : nearbyStopClusters.keySet()) {
                double distance = nearbyStopClusters.get(sc1);
                Set<TripPattern> tripPatterns1 = patternsForStopCluster(sc1);
                for (TripPattern tp0 : tripPatterns0) {
                    for (TripPattern tp1 : tripPatterns1) {
                        if (tp0 == tp1) continue;
                        P2<TripPattern> pair = new P2<TripPattern>(tp0, tp1);
                        ProfileTransfer.GoodTransferList list = transfers.get(pair);
                        if (list == null) {
                            list = new ProfileTransfer.GoodTransferList();
                            transfers.put(pair, list);
                        }
                        list.add(new ProfileTransfer(tp0, tp1, sc0, sc1, (int)distance));
                    }
                }
            }
        }
        /* Now filter the transfers down to eliminate long series of transfers in shared trunks. */
        LOG.info("Filtering out long series of transfers on trunks shared between patterns.");
        for (P2<TripPattern> pair : transfers.keySet()) {
            ProfileTransfer.GoodTransferList list = transfers.get(pair);
            TripPattern fromPattern = pair.first; // TODO consider using second (think of express-local transfers in NYC)
            Map<StopCluster, ProfileTransfer> transfersByFromCluster = Maps.newHashMap();
            for (ProfileTransfer transfer : list.good) {
                transfersByFromCluster.put(transfer.sc1, transfer);
            }
            List<ProfileTransfer> retainedTransfers = Lists.newArrayList();
            boolean inSeries = false; // true whenever a transfer existed for the last stop in the stop pattern
            for (Stop stop : fromPattern.stopPattern.stops) {
                StopCluster cluster = this.stopClusterForStop.get(stop);
                //LOG.info("stop {} cluster {}", stop, cluster.id);
                ProfileTransfer transfer = transfersByFromCluster.get(cluster);
                if (transfer == null) {
                    inSeries = false;
                    continue;
                }
                if (inSeries) continue;
                // Keep this transfer: it's not preceded by another stop with a transfer in this stop pattern
                retainedTransfers.add(transfer);
                inSeries = true;
            }
            //LOG.info("patterns {}, {} transfers", pair, retainedTransfers.size());
            for (ProfileTransfer tr : retainedTransfers) {
                transfersFromStopCluster.put(tr.sc1, tr);
                //LOG.info("   {}", tr);
            }
        }
        /*
         * for (Stop stop : transfersForStop.keys()) { System.out.println("STOP " + stop); for
         * (Transfer transfer : transfersForStop.get(stop)) { System.out.println("    " +
         * transfer.toString()); } }
         */
        LOG.info("Done finding transfers.");
    }

    /**
     * Find transfer candidates for profile routing.
     * TODO replace with an on-street search using the existing profile router functions.
     */
    public Map<StopCluster, Double> findNearbyStopClusters (StopCluster sc, double radius) {
        Map<StopCluster, Double> ret = Maps.newHashMap();
        Envelope env = new Envelope(new Coordinate(sc.lon, sc.lat));
        env.expandBy(SphericalDistanceLibrary.metersToLonDegrees(radius, sc.lat),
                SphericalDistanceLibrary.metersToDegrees(radius));
        for (StopCluster cluster : stopClusterSpatialIndex.query(env)) {
            // TODO this should account for area-like nature of clusters. Use size of bounding boxes.
            double distance = SphericalDistanceLibrary.distance(sc.lat, sc.lon, cluster.lat, cluster.lon);
            if (distance < radius) ret.put(cluster, distance);
        }
        return ret;
    }

    /* TODO: an almost similar function exists in ProfileRouter, combine these.
    *  Should these live in a separate class? */
    public List<StopAndDistance> findClosestStopsByWalking(double lat, double lon, int radius) {
        // Make a normal OTP routing request so we can traverse edges and use GenericAStar
        // TODO make a function that builds normal routing requests from profile requests
        RoutingRequest rr = new RoutingRequest(TraverseMode.WALK);
        rr.from = new GenericLocation(lat, lon);
        // FIXME requires destination to be set, not necessary for analyst
        rr.to = new GenericLocation(lat, lon);
        rr.batch = true;
        rr.setRoutingContext(graph);
        rr.walkSpeed = 1;
        rr.dominanceFunction = new DominanceFunction.LeastWalk();
        // RR dateTime defaults to currentTime.
        // If elapsed time is not capped, searches are very slow.
        rr.worstTime = (rr.dateTime + radius);
        AStar astar = new AStar();
        rr.setNumItineraries(1);
        StopFinderTraverseVisitor visitor = new StopFinderTraverseVisitor();
        astar.setTraverseVisitor(visitor);
        astar.getShortestPathTree(rr, 1); // timeout in seconds
        // Destroy the routing context, to clean up the temporary edges & vertices
        rr.rctx.destroy();
        return visitor.stopsFound;
    }

    public static class StopAndDistance {
        public Stop stop;
        public int distance;

        public StopAndDistance(Stop stop, int distance){
            this.stop = stop;
            this.distance = distance;
        }
    }

    static private class StopFinderTraverseVisitor implements TraverseVisitor {
        List<StopAndDistance> stopsFound = new ArrayList<>();
        @Override public void visitEdge(Edge edge, State state) { }
        @Override public void visitEnqueue(State state) { }
        // Accumulate stops into ret as the search runs.
        @Override public void visitVertex(State state) {
            Vertex vertex = state.getVertex();
            if (vertex instanceof TransitStop) {
                stopsFound.add(new StopAndDistance(((TransitStop) vertex).getStop(),
                        (int) state.getElapsedTimeSeconds()));
            }
        }
    }


    /** An OBA Service Date is a local date without timezone, only year month and day. */
    public BitSet servicesRunning (ServiceDate date) {
        BitSet services = new BitSet(calendarService.getServiceIds().size());
        for (AgencyAndId serviceId : calendarService.getServiceIdsOnDate(date)) {
            int n = serviceCodes.get(serviceId);
            if (n < 0) continue;
            services.set(n);
        }
        return services;
    }

    /**
     * Wraps the other servicesRunning whose parameter is an OBA ServiceDate.
     * Joda LocalDate is a similar class.
     */
    public BitSet servicesRunning (LocalDate date) {
        return servicesRunning(new ServiceDate(date.getYear(), date.getMonthOfYear(), date.getDayOfMonth()));
    }

    /** Dynamically generate the set of Routes passing though a Stop on demand. */
    public Set<Route> routesForStop(Stop stop) {
        Set<Route> routes = Sets.newHashSet();
        for (TripPattern p : patternsForStop.get(stop)) {
            routes.add(p.route);
        }
        return routes;
    }

    /**
     * Fetch upcoming vehicle departures from a stop.
     * Fetches two departures for each pattern during the next 24 hours as default
     */
    public Collection<StopTimesInPattern> stopTimesForStop(Stop stop, boolean omitNonPickups) {
        return stopTimesForStop(stop, System.currentTimeMillis()/1000, 24 * 60 * 60, 2, omitNonPickups);
    }


    public List<StopTimesInPattern> stopTimesForStop(Stop stop, long startTime, int timeRange, int numberOfDepartures, boolean omitNonPickups,
                                                     RouteMatcher routeMatcher, Integer direction, String headsign, Set<String> bannedAgencies, Set<Integer> bannedRouteTypes, boolean ignoreRealtimeUpdates) {
        return stopTimesForStop(stop, startTime, timeRange, numberOfDepartures, omitNonPickups, routeMatcher, direction,
                headsign, null, null, bannedAgencies, bannedRouteTypes, null, false, false, false);
    }

    /**
     * Fetch upcoming vehicle departures from a stop.
     * It goes though all patterns passing the stop for the previous, current and next service date.
     * It uses a priority queue to keep track of the next departures. The queue is shared between all dates, as services
     * from the previous service date can visit the stop later than the current service date's services. This happens
     * eg. with sleeper trains.
     *
     * TODO: Add frequency based trips
     * @param stop Stop object to perform the search for
     * @param startTime Start time for the search. Seconds from UNIX epoch
     * @param timeRange Searches forward for timeRange seconds from startTime
     * @param numberOfDepartures Number of departures to fetch per pattern
     * @param omitNonPickups If true, do not include vehicles that will not pick up passengers.
     * @return
     */
    // TODO: Refactor this existing mess
    public List<StopTimesInPattern> stopTimesForStop(Stop stop, long startTime, int timeRange, int numberOfDepartures, boolean omitNonPickups,
                                                     RouteMatcher routeMatcher, Integer direction, String headsign, String tripHeadsign, Stop requiredStop, Set<String> bannedAgencies, Set<Integer> bannedRouteTypes,
                                                     Collection<String> trackIds, boolean showCancelledTrips, boolean includeStopsForTrip, boolean signMode) {

    	List<StopTimesInPattern> ret = new ArrayList<>();
        
    	if (startTime <= 0)
            startTime = System.currentTimeMillis() / 1000;
    	
        Date date = new Date(startTime * 1000);
    	
        List<TripPattern> tripPatterns = null;
    	TimetableSnapshot snapshot = getTimetableSnapshot();

    	// patterns from schedule
        tripPatterns = new ArrayList<>(patternsForStop.get(stop));
        	
        // paterns added via RT
        if (snapshot != null)
        	tripPatterns.addAll(snapshot.getTripPatternsForStop(stop));
        
        for (TripPattern pattern : tripPatterns) {
            if (!routeMatcher.isEmpty() && !routeMatcher.matches(pattern.route)) {
                continue;
            }

            if (direction != null && direction != pattern.directionId) {
                continue;
            }

            if (bannedRouteTypes != null && bannedRouteTypes.contains(pattern.route.getType())) {
                continue;
            }

            if (bannedAgencies != null && bannedAgencies.contains(pattern.route.getAgency().getId())) {
                continue;
            }

            if (requiredStop != null && !pattern.getStops().contains(requiredStop)) {
                continue;
            }

            // TODO: Refactor this existing mess
            StopTimesInPattern stip = getStopTimesForPattern(pattern, date, snapshot, stop, timeRange, numberOfDepartures, omitNonPickups, headsign, 
            		tripHeadsign, trackIds, showCancelledTrips, includeStopsForTrip, signMode);

            if(stip != null)
            	ret.add(stip);
        }
        return ret;
    }
    
    // TODO: Refactor this existing mess
    public StopTimesInPattern getStopTimesForPattern(TripPattern pattern, Date date, TimetableSnapshot snapshot, Stop stop, 
    		int timeRange, int numberOfDepartures, boolean omitNonPickups,
            String headsign, String tripHeadsign, Collection<String> trackIds, boolean showCancelledTrips, boolean includeStopsForTrip, boolean signMode) {

    	ServiceDate[] serviceDates = {new ServiceDate(date).previous(), new ServiceDate(date), new ServiceDate(date).next()};

        // Use the Lucene PriorityQueue, which has a fixed size
        PriorityQueue<TripTimeShort> pq = new PriorityQueue<TripTimeShort>(numberOfDepartures) {
            @Override
            protected boolean lessThan(TripTimeShort tripTimeShort, TripTimeShort t1) {
                // Calculate exact timestamp
                return (tripTimeShort.serviceDay + tripTimeShort.realtimeDeparture) >
                        (t1.serviceDay + t1.realtimeDeparture);
            }
        };

        // Loop through all possible days
        for (ServiceDate serviceDate : serviceDates) {
            ServiceDay sd = new ServiceDay(graph, serviceDate, calendarService, pattern.route.getAgency().getId());
            int secondsSinceMidnight = sd.secondsSinceMidnight(date.getTime()/1000);

            Timetable tt = pattern.scheduledTimetable;
            if (snapshot != null)
                tt = snapshot.resolve(pattern, serviceDate);

            if (!tt.temporallyViable(sd, date.getTime()/1000, timeRange, true)) continue;

            int sidx = 0;
            for (Stop currStop : pattern.stopPattern.stops) {
                if (currStop == stop) {
                    if(omitNonPickups && pattern.stopPattern.pickups[sidx] == StopPattern.PICKDROP_NONE) continue;
                    for (TripTimes t : tt.tripTimes) {
                        if (!sd.serviceRunning(t.serviceCode)) continue;

                    	if (tripHeadsign != null && !tripHeadsign.equals(t.trip.getTripHeadsign())) continue;
                        if (headsign != null && !headsign.equals(t.getHeadsign(sidx))) continue;
                        if (trackIds != null && !trackIds.contains(t.getTrack(sidx))) continue;
                        if (hideScheduledInfo(signMode, t.isScheduled(), t.trip.getId().getAgencyId())) continue;
                        if (shouldShowDeparture(t.getDepartureTime(sidx), secondsSinceMidnight)
                                || (showCancelledTrips && shouldShowDeparture(t.getScheduledDepartureTime(sidx), secondsSinceMidnight))) {
                            pq.insertWithOverflow(new TripTimeShort(pattern, t, sidx, stop, sd, graph.getTimeZone(), includeStopsForTrip));
                        }
                    }

                    // TODO: This needs to be adapted after #1647 is merged
                    for (FrequencyEntry freq : tt.frequencyEntries) {
                        if (!sd.serviceRunning(freq.tripTimes.serviceCode)) continue;
                        int departureTime = freq.nextDepartureTime(sidx, secondsSinceMidnight);
                        if (departureTime == -1) continue;
                        int lastDeparture = freq.endTime + freq.tripTimes.getArrivalTime(sidx) -
                                freq.tripTimes.getDepartureTime(0);
                        int i = 0;
                        while (departureTime <= lastDeparture && i < numberOfDepartures) {
                            pq.insertWithOverflow(new TripTimeShort(pattern, freq.materialize(sidx, departureTime, true), sidx, stop, sd, graph.getTimeZone()));
                            departureTime += freq.headway;
                            i++;
                        }
                    }
                }
                sidx++;
            }
        }

        if (pq.size() != 0) {
            StopTimesInPattern stopTimes = new StopTimesInPattern(pattern);
            while (pq.size() != 0) {
                stopTimes.times.add(0, pq.pop());
            }
            
            return stopTimes;
        }
        
        return null;
    }

    private boolean hideScheduledInfo(boolean signMode, boolean isScheduled, String agencyId){
        if(signMode && isScheduled && (signModeHideSchedule.contains(agencyId.toUpperCase()) ||
                signModeHideSchedule.contains(agencyId.toLowerCase())) ){
            return true;
        }
        return false;
    }

    private boolean shouldShowDeparture(int departureTime, int time) {
        return departureTime != -1 && departureTime >= time;
    }

    public List<StopTimesInPattern> stopTimesForStop(Stop stop, long startTime, int timeRange, int numberOfDepartures, boolean omitNonPickups, RouteMatcher routeMatcher, String headsign) {
        return stopTimesForStop(stop, startTime, timeRange, numberOfDepartures, omitNonPickups, routeMatcher, null, headsign, null, null, false);
    }

    public List<StopTimesInPattern> stopTimesForStop(Stop stop, long startTime, int timeRange, int numberOfDepartures, boolean omitNonPickups) {
        return stopTimesForStop(stop, startTime, timeRange, numberOfDepartures, omitNonPickups, RouteMatcher.emptyMatcher(), null);
    }

    public List<StopTimesInPattern> stopTimesForStop(Stop stop, long startTime, int timeRange, int numberOfDepartures, boolean omitNonPickups, boolean signMode) {
        return stopTimesForStop(stop, startTime, timeRange, numberOfDepartures, omitNonPickups, RouteMatcher.emptyMatcher(), null, null, null, null, null, null, null, false, false, signMode);
    }

    /**
     * Get a list of all trips that pass through a stop during a single ServiceDate. Useful when creating complete stop
     * timetables for a single day.
     *
     * @param stop Stop object to perform the search for
     * @param serviceDate Return all departures for the specified date
     * @return
     */
    public List<StopTimesInPattern> getStopTimesForStop(Stop stop, ServiceDate serviceDate, boolean omitNonPickups, boolean ignoreRealtimeUpdates) {
        List<StopTimesInPattern> ret = new ArrayList<>();
        TimetableSnapshot snapshot = null;
        if (graph.timetableSnapshotSource != null && !ignoreRealtimeUpdates) {
            snapshot = graph.timetableSnapshotSource.getTimetableSnapshot();
        }
        Collection<TripPattern> patterns = patternsForStop.get(stop);
        for (TripPattern pattern : patterns) {
            StopTimesInPattern stopTimes = new StopTimesInPattern(pattern);
            Timetable tt;
            if (snapshot != null && !ignoreRealtimeUpdates){
                tt = snapshot.resolve(pattern, serviceDate);
            } else {
                tt = pattern.scheduledTimetable;
            }
            ServiceDay sd = new ServiceDay(graph, serviceDate, calendarService, pattern.route.getAgency().getId());
            int sidx = 0;
            for (Stop currStop : pattern.stopPattern.stops) {
                if (currStop == stop) {
                    if(omitNonPickups && pattern.stopPattern.pickups[sidx] == StopPattern.PICKDROP_NONE) continue;
                    for (TripTimes t : tt.tripTimes) {
                        if (!sd.serviceRunning(t.serviceCode)) continue;
                        stopTimes.times.add(new TripTimeShort(pattern, t, sidx, stop, sd, graph.getTimeZone()));
                    }
                }
                sidx++;
            }
            ret.add(stopTimes);
        }
        return ret;
    }

    /**
     * Get parent stop for given stop.
     */
    public Stop getParentStopForStop(Stop stop) {
        String ps = stop.getParentStation();
        if (ps == null) {
            return null;
        }
        AgencyAndId pid = new AgencyAndId(stop.getId().getAgencyId(), ps);
        return graph.parentStopById.get(pid);
    }

    /** Fetch a cache of nearby intersection distances for every transit stop in this graph, lazy-building as needed. */
    public StopTreeCache getStopTreeCache() {
        if (stopTreeCache == null) {
            synchronized (this) {
                if (stopTreeCache == null) {
                    stopTreeCache = new StopTreeCache(graph, MAX_WALK_METERS); // TODO make this max-distance variable
                }
            }
        }
        return stopTreeCache;
    }

    /**
     * Get the most up-to-date timetable for the given TripPattern, as of right now.
     * There should probably be a less awkward way to do this that just gets the latest entry from the resolver without
     * making a fake routing request.
     */
    public Timetable currentUpdatedTimetableForTripPattern (TripPattern tripPattern, ServiceDate serviceDate) {
        RoutingRequest req = new RoutingRequest();
        req.setRoutingContext(graph, (Vertex)null, (Vertex)null);
        // The timetableSnapshot will be null if there's no real-time data being applied.
        if (req.rctx.timetableSnapshot == null) return tripPattern.scheduledTimetable;
        return req.rctx.timetableSnapshot.resolve(tripPattern, serviceDate);
    }

    public Timetable currentUpdatedTimetableForTripPattern (TripPattern tripPattern) {
        // Get the updated times for right now, which is the only reasonable default since no date is supplied.
        ServiceDate serviceDate = new ServiceDate(getTime());
        return currentUpdatedTimetableForTripPattern(tripPattern, serviceDate);
    }

    /**
     * Stop clusters can be built in one of two ways, either by geographical proximity, or
     * according to a parent/child station topology, if it exists.
     *
     * Some challenges faced by DC and Trimet:
     * FIXME OBA parentStation field is a string, not an AgencyAndId, so it has no agency/feed scope
     * But the DC regional graph has no parent stations pre-defined, so no use dealing with them for now.
     * However Trimet stops have "landmark" or Transit Center parent stations, so we don't use the parent stop field.
     *
     * We can't use a similarity comparison, we need exact matches. This is because many street names differ by only
     * one letter or number, e.g. 34th and 35th or Avenue A and Avenue B.
     * Therefore normalizing the names before the comparison is essential.
     * The agency must provide either parent station information or a well thought out stop naming scheme to cluster
     * stops -- no guessing is reasonable without that information.
     */
    public void clusterStops() {
        if (graph.stopClusterMode != null) {
            switch (graph.stopClusterMode) {
                case "parentStation":
                    clusterByParentStation();
                    break;
                case "proximity":
                    clusterByProximity();
                    break;
                default:
                    clusterByProximity();
            }
        } else {
            clusterByProximity();
        }
    }

    private void clusterByProximity() {
        int psIdx = 0; // unique index for next parent stop
        LOG.info("Clustering stops by geographic proximity and name...");
        // Each stop without a cluster will greedily claim other stops without clusters.
        for (Stop s0 : stopForId.values()) {
        	if(s0.getName() == null) {
        		LOG.warn("Stop " + s0.getId() + " has an empty name, and so cannot be clustered by name; skipping.");
        		continue;
        	}
            if (stopClusterForStop.containsKey(s0) 
            		|| s0.getLat() == StopTime.MISSING_VALUE || s0.getLon() == StopTime.MISSING_VALUE) continue; // skip stops that have already been claimed by a cluster
            String s0normalizedName = StopNameNormalizer.normalize(s0.getName());
            StopCluster cluster = new StopCluster(String.format("C%03d", psIdx++), s0normalizedName);
            // LOG.info("stop {}", s0normalizedName);
            // No need to explicitly add s0 to the cluster. It will be found in the spatial index query below.
            Envelope env = new Envelope(new Coordinate(s0.getLon(), s0.getLat()));
            env.expandBy(SphericalDistanceLibrary.metersToLonDegrees(CLUSTER_RADIUS, s0.getLat()),
                    SphericalDistanceLibrary.metersToDegrees(CLUSTER_RADIUS));
            for (TransitStop ts1 : stopSpatialIndex.query(env)) {
                Stop s1 = ts1.getStop();
                double geoDistance = SphericalDistanceLibrary.fastDistance(
                        s0.getLat(), s0.getLon(), s1.getLat(), s1.getLon());
                if (geoDistance < CLUSTER_RADIUS) {
                    String s1normalizedName = StopNameNormalizer.normalize(s1.getName());
                    // LOG.info("   --> {}", s1normalizedName);
                    // LOG.info("       geodist {} stringdist {}", geoDistance, stringDistance);
                    if (s1normalizedName.equals(s0normalizedName)) {
                        // Create a bidirectional relationship between the stop and its cluster
                        cluster.children.add(s1);
                        stopClusterForStop.put(s1, cluster);
                    }
                }
            }
            cluster.computeCenter();
            stopClusterForId.put(cluster.id, cluster);
        }
    }
    private void clusterByParentStation() {
        LOG.info("Clustering stops by parent station...");
        for (Stop stop : stopForId.values()) {
            String ps = stop.getParentStation();
            if (ps == null || ps.isEmpty()) {
                continue;
            }
            StopCluster cluster;
            if (stopClusterForId.containsKey(ps)) {
                cluster = stopClusterForId.get(ps);
            } else {
                cluster = new StopCluster(ps, stop.getName());
                Stop parent = graph.parentStopById.get(new AgencyAndId(stop.getId().getAgencyId(), ps));
                cluster.setCoordinates(parent.getLat(), parent.getLon());
                stopClusterForId.put(ps, cluster);

            }
            cluster.children.add(stop);
            stopClusterForStop.put(stop, cluster);
        }
    }

    public Response getGraphQLResponse(String query, Map<String, Object> variables, String operationName) {
        ExecutionResult executionResult = graphQL.execute(query, operationName, null, variables);
        Response.ResponseBuilder res = Response.status(Response.Status.OK);
        HashMap<String, Object> content = new HashMap<>();
        if (!executionResult.getErrors().isEmpty()) {
            res = Response.status(Response.Status.INTERNAL_SERVER_ERROR);
            content.put("errors", executionResult.getErrors());
        }
        if (executionResult.getData() != null) {
            content.put("data", executionResult.getData());
        }
        return res.entity(content).build();
    }

    /**
     * Fetch an agency by its string ID, ignoring the fact that this ID should be scoped by a feedId.
     * This is a stopgap (i.e. hack) method for fetching agencies where no feed scope is available.
     * I am creating this method only to allow merging pull request #2032 which adds GraphQL.
     * Note that if the same agency ID is defined in several feeds, this will return one of them
     * at random. That is obviously not the right behavior. The problem is that agencies are
     * not currently keyed on an AgencyAndId object, but on separate feedId and id Strings.
     * A real fix will involve replacing or heavily modifying the OBA GTFS loader, which is now
     * possible since we have forked it.
     */
    public Agency getAgencyWithoutFeedId(String agencyId) {
        // Iterate over the agency map for each feed.
        for (Map<String, Agency> agencyForId : agenciesForFeedId.values()) {
            Agency agency = agencyForId.get(agencyId);
            if (agency != null) {
                return agency;
            }
        }
        return null;
    }

    /**
     * Construct a set of all Agencies in this graph, spanning across all feed IDs.
     * I am creating this method only to allow merging pull request #2032 which adds GraphQL.
     * This should probably be done some other way, see javadoc on getAgencyWithoutFeedId.
     */
    public Set<Agency> getAllAgencies() {
        Set<Agency> allAgencies = new HashSet<>();
        for (Map<String, Agency> agencyForId : agenciesForFeedId.values()) {
            allAgencies.addAll(agencyForId.values());
        }
        return allAgencies;
    }

    private TimetableSnapshot getTimetableSnapshot() {
        if (graph.timetableSnapshotSource != null) {
            return graph.timetableSnapshotSource.getTimetableSnapshot();
        }
        return null;
    }

    public Trip getTripForId(AgencyAndId id) {
        if (tripForId.containsKey(id)) {
            return tripForId.get(id);
        }
        return findAddedTripInfo(id).getValue();
    }

    public TripPattern getTripPatternForTripId(AgencyAndId id) {
        if (tripForId.containsKey(id)) {
            Trip trip = tripForId.get(id);
            return patternForTrip.get(trip);
        }
        return findAddedTripInfo(id).getKey();
    }

    private Pair<TripPattern, Trip> findAddedTripInfo(AgencyAndId id) {
        TimetableSnapshot snapshot = getTimetableSnapshot();
        if (snapshot != null) {
            // check today, tomorrow, and yesterday
            ServiceDate today = new ServiceDate(getTime());
            for (ServiceDate sd : Arrays.asList(today, today.next(), today.previous())) {
                TripPattern tripPattern = snapshot.getLastAddedTripPattern(id.getAgencyId(), id.getId(), sd);
                if (tripPattern != null) {
                    Timetable tt = snapshot.resolve(tripPattern, sd);
                    for (TripTimes tripTimes : tt.tripTimes) {
                        if (tripTimes.trip.getId().equals(id)) {
                            return new Pair<>(tripPattern, tripTimes.trip);
                        }
                    }
                }
            }
        }
        return new Pair<>(null, null);
    }

    private Date getTime() {
        return time != -1 ? new Date(time) : Calendar.getInstance().getTime();
    }

    public void setTime(long time) {
        this.time = time;
    }
}