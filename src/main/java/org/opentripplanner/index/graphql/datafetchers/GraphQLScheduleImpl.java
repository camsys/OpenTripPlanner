package org.opentripplanner.index.graphql.datafetchers;

import graphql.schema.DataFetchingEnvironment;
import org.joda.time.DateTime;
import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.gtfs.model.Stop;
import org.onebusaway.gtfs.model.Trip;
import org.onebusaway.gtfs.model.calendar.ServiceDate;
import org.opentripplanner.api.model.*;
import org.opentripplanner.api.resource.GraphPathToTripPlanConverter;
import org.opentripplanner.gtfs.GtfsLibrary;
import org.opentripplanner.index.graphql.GraphQLRequestContext;
import org.opentripplanner.index.model.StopTimesForPatternsQuery;
import org.opentripplanner.index.model.StopTimesInPattern;
import org.opentripplanner.index.model.TripTimeShort;
import org.opentripplanner.routing.algorithm.GenericDijkstra;
import org.opentripplanner.routing.algorithm.strategies.SkipEdgeStrategy;
import org.opentripplanner.routing.algorithm.strategies.SkipTraverseResultStrategy;
import org.opentripplanner.routing.core.*;
import org.opentripplanner.routing.edgetype.*;
import org.opentripplanner.routing.graph.Edge;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.routing.spt.GraphPath;
import org.opentripplanner.routing.spt.ShortestPathTree;
import org.opentripplanner.routing.trippattern.RealTimeState;
import org.opentripplanner.routing.vertextype.*;
import org.opentripplanner.standalone.Router;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class GraphQLScheduleImpl {

	private static final int DEFAULT_MAX_RESULTS = 20;
	private static final int DEFAULT_NUM_DEPARTURES = 10;
	private static final int DEFAULT_TIME_RANGE_HOURS = 8;
	private static final int DEFAULT_MAX_TIME_MINUTES = 8 * 60; // 8 hrs
	private static final int MAX_TIME_HARD_CAP_MINUTES = 1440; //24hrs
	private static final int MAX_RESULTS_HARD_CAP = 30;

	private static final Logger LOG = LoggerFactory.getLogger(GraphQLScheduleImpl.class);

	public List<Object> getSchedule(DataFetchingEnvironment environment) {

		Graph graph = getRouter(environment).graph;

		GraphQLQueryTypeInputs.GraphQLQueryTypeScheduleArgsInput input =
				new GraphQLQueryTypeInputs.GraphQLQueryTypeScheduleArgsInput(environment.getArguments());

		// GraphQL Arguments
		Stop fromStop = graph.index.stopForId.get(
				AgencyAndId.convertFromString(input.getGraphQLFromGtfsId()));

		Stop toStop = graph.index.stopForId.get(
				AgencyAndId.convertFromString(input.getGraphQLToGtfsId()));

		long time = System.currentTimeMillis();
		if(input.getGraphQLTime() != null) {
			time = new DateTime(input.getGraphQLTime()).getMillis();
		}

		long maxTimeOffset = TimeUnit.MINUTES.toMillis(DEFAULT_MAX_TIME_MINUTES);
		if (input.getGraphQLMaxTime() != null) {
			maxTimeOffset = TimeUnit.MINUTES.toMillis(input.getGraphQLMaxTime());
			//Hard cap max time at 24 hours
			maxTimeOffset = Math.min(maxTimeOffset, TimeUnit.MINUTES.toMillis(MAX_TIME_HARD_CAP_MINUTES));
		}

		// Get list of stop times in pattern using from stop
		List<StopTimesInPattern> stips = getStopTimesInPattern(graph, time, fromStop);


		// Filter list of stips by looking for patterns with trips that either have dest stop
		// or have transfers that lead to dest stop
		List<StopTimesInPattern> filteredStipsFrom = filterPatternStops(stips, fromStop, toStop, graph);

		if(!filteredStipsFrom.isEmpty()){
			stips = filteredStipsFrom;
		}

		// Store departures by trip Id
		Map<AgencyAndId, Set<Long>> departuresByTripId = new HashMap<>();

		// Store cancelled trip info
		Map <String, Set<AgencyAndId>> cancelledTripsByDepartureTime = new HashMap<>();

		// Store hold info
		Map<AgencyAndId, String> tripToRealTimeSignText = new HashMap<>();


		// Loop through stop times in pattern and filter departure times that don't fall in range
		// Also populate cancelled trip info
		for(StopTimesInPattern stip : stips) {
			for(TripTimeShort tts : stip.times) {

				long departureTime = TimeUnit.SECONDS.toMillis(tts.serviceDay + tts.scheduledDeparture);

				// Update filtered departure times
				updateValidDepartureTimesList(departureTime, time, tts.tripId, departuresByTripId, maxTimeOffset);

				// Update Cancelled trips info
				updateCancelledTripsList(cancelledTripsByDepartureTime, tts.realtimeState, tts.serviceDay,
						departureTime, tts.tripId);

				// Update Hold text info
				updateHoldTextInfo(tripToRealTimeSignText, tts.realtimeSignText, tts.tripId);

			}
		}

		TreeSet<Long> uniqueDepartureTimes = processUniqueDepartureTimes(stips, departuresByTripId,
				fromStop, toStop, graph);

		// Set max results
		final int maxResults;
		if(input.getGraphQLMaxResults() != null
				&& input.getGraphQLMaxResults() > 0) {
			maxResults = Math.min(input.getGraphQLMaxResults(), MAX_RESULTS_HARD_CAP);
		} else {
			maxResults = DEFAULT_MAX_RESULTS;
		}

		Map<String, Set<Itinerary>> itinerariesByDepartureTime = new ConcurrentHashMap<>();
		final int searchLimit;
		if (input.getGraphQLMaxResults() == null && input.getGraphQLMaxTime() != null) {
			searchLimit = 1000;
			//arbitrarily large limit for intermediate search
		} else {
			searchLimit = maxResults;
		}

		// Loop through first several results using a parallel stream for speed
		uniqueDepartureTimes.parallelStream().limit(searchLimit).forEach(departureTime -> {
			// Loop through all of the unique departure times and find itineraries for each departure time
			processItinerariesForDepartureTime( graph,
					itinerariesByDepartureTime,
					environment,
					departureTime,
					fromStop,
					toStop);
		});

		// If not enough results are found, then revert to using a non multi-threaded approach to preserve order
		uniqueDepartureTimes.stream().skip(searchLimit).forEach(departureTime -> {
			if(itinerariesByDepartureTime.size() >= searchLimit) {
				return;
			}
			processItinerariesForDepartureTime(
					graph,
					itinerariesByDepartureTime,
					environment,
					departureTime,
					fromStop,
					toStop);
		});

		// Map to store single intinerary if multiple are found with the same arrival time
		Map<Long, Itinerary> itineraryFilteredByArrival = new HashMap<>();
		filterItinerariesByArrivalTime(itineraryFilteredByArrival, itinerariesByDepartureTime);

		List<String> departuresSorted = itinerariesByDepartureTime.keySet()
				.stream()
				.sorted()
				.collect(Collectors.toList());

		List<Object> result = generateResultsForItineraries(departuresSorted, itinerariesByDepartureTime,
				itineraryFilteredByArrival, cancelledTripsByDepartureTime,
				tripToRealTimeSignText, maxTimeOffset);

		result.stream().limit(maxResults).collect(Collectors.toList());

		return result;
	}


	private List<StopTimesInPattern> getStopTimesInPattern(Graph graph, long time, Stop fromStop){
		// Get all the stopTimes starting from the fromStop
		long startTime = TimeUnit.MILLISECONDS.toSeconds(time);
		int timeRange = (int) TimeUnit.HOURS.toSeconds(DEFAULT_TIME_RANGE_HOURS);
		int numberOfDepartures = DEFAULT_NUM_DEPARTURES;
		boolean omitNonPickups = true;

		StopTimesForPatternsQuery query = new StopTimesForPatternsQuery
				.Builder(fromStop, startTime, timeRange, numberOfDepartures, omitNonPickups)
				.showCancelledTrips(true)
				.includeTripPatterns(true)
				.build();

		return graph.index.stopTimesForStop(query);
	}

	private List<StopTimesInPattern> filterPatternStops(List<StopTimesInPattern> stips,
														Stop fromStop,
														Stop toStop,
														Graph graph) {
		List<StopTimesInPattern> filteredStipsFrom = new ArrayList();

		// Loop through all from stop patterns
		for(StopTimesInPattern stip : stips) {
			List<Trip> patternTrips = stip.patternFull.getTrips();
			List<Stop> stops = stip.patternFull.getStops();
			int transfersCount = 0;
			if(shouldAddStopPatternStops(patternTrips, stops, fromStop, toStop, graph, transfersCount)){
				filteredStipsFrom.add(stip);
			}
		}
		return filteredStipsFrom;
	}

	private boolean shouldAddStopPatternStops(List<Trip> patternTrips,
											  List<Stop> stops,
											  Stop fromStop,
											  Stop destinationStop,
											  Graph graph,
											  int transferCount){

		// Found Stop Flag
		boolean foundStop = false;

		// Go through all the stops in the pattern/trip
		for(Stop stop : stops){
			// if the BOARDING stop is found then we start working from there
			if(!foundStop && stop.getId().equals(fromStop.getId())){
				foundStop = true;
			}
			// else process stops after the BOARDING stop
			else if(foundStop){
				// if DESTINATION stop is on same pattern then return true
				if(stop.getId().equals(destinationStop.getId())){
					return true;
				}
				// check transfers to see if we find the destination stops there
				// limit to 3 transfers
				if(transferCount < 3) {
					// check to see if one of the stops after the BOARDING stop has any transfers
					boolean hasStopTransfer = graph.getTransferTable().hasStopTransfer(stop, stop);
					// if a stop is found with a process check to see if it leads to our DESTINATION stop
					if (hasStopTransfer) {
						// check all pattern trips to see which trips have a transfer for CURRENT STOP
						// if any of them have a transfer then recurse
						for (Trip patternTrip : patternTrips) {

							// Get list of trips that you can transfer to
							Set<Trip> transferTrips = getTransferTrips(graph, stop, patternTrip, fromStop);

							if (transferTrips.isEmpty() && stop.getId().getAgencyId().equals("MNR")) {
								return true;
							}
							// Recurse through transfer trips to see if any of them lead to the DESTINATION stop
							for (Trip transferTrip : transferTrips) {
								List<Stop> tripStops = graph.index.patternForTrip.get(transferTrip).getStops();
								List<Trip> trips = (Collections.singletonList(transferTrip));
								if (shouldAddStopPatternStops(trips, tripStops, stop, destinationStop, graph, transferCount + 1)) {
									return true;
								}
							}
						}
					}
				}
			}
		}
		return false;
	}

	private void updateValidDepartureTimesList(long departureTime,
											   long currentTime,
											   AgencyAndId tripId,
											   Map<AgencyAndId,
													   Set<Long>> departuresByTripId,
											   long maxTimeOffset){

		boolean departureTimeBeforeCurrentTime = departureTime < currentTime;
		boolean departureTimeAfterMaxTime = maxTimeOffset!=0 && departureTime > (maxTimeOffset + currentTime);

		if(departureTimeBeforeCurrentTime || departureTimeAfterMaxTime) {
			return;
		}

		Set<Long> departureTimes = departuresByTripId.get(tripId);
		if(departureTimes == null){
			departureTimes = new HashSet<>();
			departuresByTripId.put(tripId,departureTimes);
		}
		departureTimes.add(departureTime);
	}

	private void updateCancelledTripsList(Map<String, Set<AgencyAndId>> cancelledTripsByDepartureTime,
										  RealTimeState stopRealtimeState,
										  long serviceDay,
										  long departureTime,
										  AgencyAndId cancelledTripId){

		if(stopRealtimeState.equals(RealTimeState.CANCELED)){
			ServiceDate serviceDate = new ServiceDate(new Date(serviceDay));
			Set<AgencyAndId> tripIds = cancelledTripsByDepartureTime.get(departureTime);
			if(tripIds == null) {
				tripIds = new HashSet<>();
				cancelledTripsByDepartureTime.put(serviceDate.getAsString(), tripIds);
			}
			tripIds.add(cancelledTripId);
		}
	}

	private void updateHoldTextInfo(Map<AgencyAndId, String> tripToRealTimeSignText, String realtimeSignText,
									AgencyAndId tripId) {
		tripToRealTimeSignText.put(tripId, realtimeSignText);
	}

	private TreeSet<Long> processUniqueDepartureTimes(List<StopTimesInPattern> filteredStips,
													  Map<AgencyAndId, Set<Long>> departuresByTripId,
													  Stop fromStop,
													  Stop toStop,
													  Graph graph) {

		TreeSet<Long> uniqueDepartureTimes = new TreeSet<>();

		// Loop through all from stop patterns
		for(StopTimesInPattern filteredStip : filteredStips) {
			List<Trip> patternTrips = filteredStip.patternFull.getTrips();
			List<Stop> stops = filteredStip.patternFull.getStops();
			populateUniqueDepartureTimesForStip(uniqueDepartureTimes, departuresByTripId, patternTrips, stops,
					fromStop, toStop, graph);
		}

		return uniqueDepartureTimes;
	}

	private void populateUniqueDepartureTimesForStip(TreeSet<Long> uniqueDepartureTimes,
													 Map<AgencyAndId, Set<Long>> departuresByTripId,
													 List<Trip> patternTrips,
													 List<Stop> stops,
													 Stop fromStop,
													 Stop destinationStop,
													 Graph graph){


		// Found Stop Flag
		boolean foundStop = false;

		// First Scenario : check for case where there are no transfers
		// much faster so we want to check for this first
		int stopIndex = 0;
		for(Stop stop : stops){
			// if the BOARDING stop is found then we start working from there
			if(!foundStop){
				if(stop.getId().equals(fromStop.getId())){
					foundStop = true;
				} else {
					stopIndex++;
				}

			}
			// if the DEPARTURE stop is on the same pattern then filter out
			// trips by departure time
			else if(foundStop && stop.getId().equals(destinationStop.getId())){
				for(Trip patternTrip : patternTrips){
					// Filter trips to make sure they have a valid departure time
					if(departuresByTripId.containsKey(patternTrip.getId())){
						uniqueDepartureTimes.addAll(departuresByTripId.get(patternTrip.getId()));
					}
				}
				return;
			}
		}

		// Second Scenario: check for case where there may be transfers
		Set<Trip> addedTrips = new HashSet<>();
		for(int i=stopIndex; i<stops.size(); i++){
			Stop stop = stops.get(i);
			// check to see if one of the stops after the BOARDING stop has any transfers
			boolean hasStopTransfer = graph.getTransferTable().hasStopTransfer(stop, stop);
			// if a stop is found with a process check to see if it leads to our DESTINATION stop
			if (hasStopTransfer) {
				// check all pattern trips to see which trips have a transfer for CURRENT STOP
				// if any of them have a transfer then recurse
				for (Trip patternTrip : patternTrips) {
					// Filter trips to make sure they have a valid departure time
					// Also check to see if trip has already been added
					if(!departuresByTripId.containsKey(patternTrip.getId()) || addedTrips.contains(patternTrip)){
						continue;
					}
					// Get list of trips that you can transfer to
					Set<Trip> transferTrips = getTransferTrips(graph, stop, patternTrip, fromStop);

					if(transferTrips.isEmpty()){
						uniqueDepartureTimes.addAll(departuresByTripId.get(patternTrip.getId()));
						addedTrips.add(patternTrip);
					}
					// Recurse through transfer trips to see if any of them lead to the DESTINATION stop
					else {
						populateUniqueTimesForTransfer(graph, transferTrips, stop, destinationStop, uniqueDepartureTimes,
								departuresByTripId, addedTrips, patternTrip);
					}



				}
			}
		}
	}

	private void populateUniqueTimesForTransfer(Graph graph,
												Set<Trip> transferTrips,
												Stop stop,
												Stop destinationStop,
												TreeSet<Long> uniqueDepartureTimes,
												Map<AgencyAndId, Set<Long>> departuresByTripId,
												Set<Trip> addedTrips,
												Trip patternTrip){
		for (Trip transferTrip : transferTrips) {
			List<Stop> tripStops = graph.index.patternForTrip.get(transferTrip).getStops();
			List<Trip> trips = (Collections.singletonList(transferTrip));
			if(shouldAddStopPatternStops(trips, tripStops, stop, destinationStop, graph, 1)){
				uniqueDepartureTimes.addAll(departuresByTripId.get(patternTrip.getId()));
				addedTrips.add(patternTrip);
				return;
			}
		}
	}

	private Set<Trip> getTransferTrips(Graph graph, Stop stop, Trip patternTrip, Stop fromStop){
		Set<Trip> transferTrips = new HashSet<>();

		// Get list of trips that you can transfer to
		TransferTable transferTable = graph.getTransferTable();

		// Check for preferred transfers
		List<AgencyAndId> transferTripIds = transferTable
				.getPreferredTransfers(stop.getId(), stop.getId(), patternTrip, fromStop);

		// Add all preferred transfers
		if(!transferTripIds.isEmpty()){
			for (AgencyAndId transferTripId : transferTripIds) {
				Trip transferTrip = graph.index.tripForId.get(transferTripId);
				transferTrips.add(transferTrip);
			}
		}
		// check to see if stop has any transfers
		// if it does add all the trips
		else if(transferTable.hasStopTransfer(stop, stop)){
			// get all trips that has that stop
			Collection<TripPattern> transferTripPatterns = graph.index.patternsForStop.get(stop);
			// add all trips that have transfers for that pattern
			for(TripPattern tripPattern : transferTripPatterns){
				transferTrips.addAll(tripPattern.getTrips());
			}
			if(transferTrips.contains(patternTrip)){
				transferTrips.remove(patternTrip);
			}

		}

		return transferTrips;
	}

	private void filterItinerariesByArrivalTime(Map<Long, Itinerary> itineraryFilteredByArrival,
												Map<String, Set<Itinerary>> itinerariesByDepartureTime){
		for(Map.Entry<String, Set<Itinerary>> entry :  itinerariesByDepartureTime.entrySet()){
			for(Itinerary arrivalItinerary : entry.getValue()){
				Long endTime = arrivalItinerary.endTime.getTimeInMillis();
				Itinerary departureItinerary = itineraryFilteredByArrival.get(endTime);
				if(departureItinerary == null || arrivalItinerary.duration < departureItinerary.duration){
					itineraryFilteredByArrival.put(endTime, arrivalItinerary);
				}
			}
		}
	}


	private void processItinerariesForDepartureTime( Graph graph,
													 Map<String, Set<Itinerary>> itinerariesByDepartureTime,
													 DataFetchingEnvironment environment,
													 Long departureTime,
													 Stop fromStop,
													 Stop toStop){


		// New Routing Request
		// Get 2 itineraries for each request
		RoutingRequest rr = new RoutingRequest();
		rr.setDateTime(new Date(departureTime - 1));
		rr.setNumItineraries(1);
		rr.setMode(TraverseMode.RAIL);
		rr.allowUnknownTransfers = false;
		rr.setBannedAgencies("MTABC, MTA NYCT, MTASBWY");
		rr.setBannedRouteTypes("3,702,1");
		rr.setOptimize(OptimizeType.TRANSFERS);
		rr.setRoutingContext(graph, graph.index.stopVertexForStop.get(fromStop), graph.index.stopVertexForStop.get(toStop));

		GenericDijkstra gd = new GenericDijkstra(rr);

		gd.setSkipEdgeStrategy(getScheduleSkipEdgeStrategy(null, fromStop.getId().getAgencyId()));
		gd.setSkipTraverseResultStrategy(getSkipTraverseResultStrategy());

		State initialState = new State(rr);
		ShortestPathTree spt = gd.getShortestPathTree(initialState);

		if (spt.getPaths().isEmpty())
			return;

		// Generate an itinerary for each path (at most 2) and add to itinerariesByDepartureTime
		for (GraphPath path : spt.getPaths()) {
			Itinerary i = GraphPathToTripPlanConverter.generateItinerary(path, true, true, Locale.ENGLISH);

			// check to make sure we didn't already add that itinerary
			// if not added then add to itinerariesByDepartureTime
			synchronized (itinerariesByDepartureTime) {
				Set<Itinerary> itineraries = itinerariesByDepartureTime.get(i.startTimeFmt);
				if (itineraries == null)
					itineraries = new HashSet<>();

				// itineraries don't implement Comparable, so a hack:
				String thisItineraryHash = itineraryHash(i);
				List<String> existingItineraryHashes = itineraries.stream()
						.map(itin -> itineraryHash(itin))
						.collect(Collectors.toList());

				if (!existingItineraryHashes.contains(thisItineraryHash))
					itineraries.add(i);

				itinerariesByDepartureTime.put(i.startTimeFmt, itineraries);
			}

		}

	}

	private SkipTransferTraverseResultStrategy getSkipTraverseResultStrategy(){
		return new SkipTransferTraverseResultStrategy();
	}

	private class SkipTransferTraverseResultStrategy implements SkipTraverseResultStrategy {

		@Override
		public boolean shouldSkipTraversalResult(Vertex origin, Vertex target, State parent, State current,
												 ShortestPathTree spt, RoutingRequest traverseOptions) {

			try {
				if (current != null && parent != null) {
					AgencyAndId currentTripId = current.getTripId();
					AgencyAndId parentTripId = parent.getTripId();

					boolean bothTripsNull = currentTripId == null && parentTripId == null;
					boolean tripsNotEqual =  currentTripId != null && parentTripId != null && !currentTripId.equals(parentTripId);


					if(!bothTripsNull && (parentTripId == null || tripsNotEqual)) {
						AgencyAndId currentRoute = current.getRoute();
						AgencyAndId parentRoute = parent.getRoute();

						boolean hasNullRoute = currentRoute == null || parentRoute == null;
						boolean routesNotEqual = !hasNullRoute && current.getRoute().getId().equals(parent.getRoute().getId());

						if (routesNotEqual){
							return true;
						}
					}
				}
			}catch (Exception e){
				LOG.error("unable to skip traversale for current trip {} and parent trip {}",current.getTripId(), parent.getTripId(),e);
			}
			return false;
		}
	}

	private SkipEdgeStrategy getScheduleSkipEdgeStrategy(Set<Edge> edges, String agencyId){
		return new MatchPatternSkipEdgeStrategy(edges, agencyId);
	}

	private Set<Edge> getEdgesForItineraries(GraphPath path) {
		return path.edges.stream().collect(Collectors.toSet());
	}

	private String itineraryHash(Itinerary itin) {
		return String.join("", itin.legs.stream()
				.map(leg -> leg.startTimeFmt + leg.endTimeFmt + leg.from.stopId + leg.to.stopId)
				.collect(Collectors.toList()));
	}

	private class MatchPatternSkipEdgeStrategy extends OneAgencySkipEdgeStrategy implements SkipEdgeStrategy {
		private Set<Edge> edges;

		MatchPatternSkipEdgeStrategy(Set<Edge> edges, String agencyId) {
			super(agencyId);
			this.edges = edges;
		}

		@Override
		public boolean shouldSkipEdge(Vertex origin, Vertex target, State current, Edge edge, ShortestPathTree spt,
									  RoutingRequest traverseOptions) {
			if(super.shouldSkipEdge(origin, target, current, edge, spt, traverseOptions) || (edges != null && !edges.contains(edge)))
				return true;

			return false;
		}

	}

	private class OneAgencySkipEdgeStrategy implements SkipEdgeStrategy {
		private String agencyId;

		OneAgencySkipEdgeStrategy(String agencyId) {
			this.agencyId = agencyId;
		}

		@Override
		public boolean shouldSkipEdge(Vertex origin, Vertex target, State current, Edge edge, ShortestPathTree spt,
									  RoutingRequest traverseOptions) {

			if(edge instanceof StreetEdge || edge instanceof StreetTransitLink || origin.equals(target))
				return true;

			if(edge instanceof TransferEdge || edge instanceof LandmarkEdge) {
				if(edge.getFromVertex() instanceof TransitStop) {
					if(!GtfsLibrary.convertIdFromString(edge.getFromVertex().getLabel())
							.getAgencyId().equals(agencyId))
						return true;
				}

				if(edge.getToVertex() instanceof TransitStop) {
					if(!GtfsLibrary.convertIdFromString(edge.getToVertex().getLabel())
							.getAgencyId().equals(agencyId))
						return true;
				}
			}
			return false;
		}

	}

	private List<Object> generateResultsForItineraries(List<String> departuresSorted,
													   Map<String, Set<Itinerary>> itinerariesByDepartureTime,
													   Map<Long, Itinerary> itineraryFilteredByArrival,
													   Map <String, Set<AgencyAndId>> cancelledTripsByDepartureTime,
													   Map<AgencyAndId, String> tripToRealTimeSignText,
													   long maxTimeOffset) {
		List<Object> result = new ArrayList<>();
		for(String key : departuresSorted) {
			for(Itinerary itin : itinerariesByDepartureTime.get(key)) {
				Itinerary endItinerary = itineraryFilteredByArrival.get(itin.endTime.getTimeInMillis());
				if (maxTimeOffset!=0 && itin.startTime.getTime().getTime() > (maxTimeOffset + System.currentTimeMillis())) {
					continue;
				}
				if(endItinerary != null && !itin.equals(endItinerary) && itin.transfers >= endItinerary.transfers){
					continue;
				}
				Map<String, Object> itineraryOut = new HashMap<>();
				itineraryOut.put("transfers", itin.transfers);
				itineraryOut.put("durationSeconds", itin.duration);

				try {
					List<Map<String, Object>> legs = getMappedLegProperties(itin.legs, cancelledTripsByDepartureTime,
							tripToRealTimeSignText);
					itineraryOut.put("legs", legs);
					result.add(itineraryOut);
				} catch (Exception e){
					LOG.warn("Unable to add legs to result list");
					continue;
				}
			}
		}
		return result;
	}

					// Trip Info
					legOut.put("routeLongName", leg.routeLongName);
					legOut.put("routeId", leg.routeId);
					legOut.put("headsign", leg.headsign);
					legOut.put("tripShortName", leg.tripShortName);
					legOut.put("tripId", leg.tripId);
					legOut.put("direction", leg.tripDirectionId);
					legOut.put("destination", leg.stopHeadsign);
					legOut.put("from", leg.from.stopId);
					legOut.put("to", leg.to.stopId);
					legOut.put("stops", getStops(leg.stop, leg.from, leg.to));
					legOut.put("track", leg.from.track);
					legOut.put("stopNote", leg.from.note);
					legOut.put("occupancy", getOccupancy(leg.vehicleInfo));
					legOut.put("carriages", getCarriages(leg.vehicleInfo));
					legOut.put("alerts", leg.alerts);
					legOut.put("cancelled", isTripCancelled(leg.serviceDate, leg.tripId, cancelledTripsByDepartureTime));
					legOut.put("hold", isHeld(tripToRealTimeSignText, leg.tripId));

		for(Leg leg : itinLegs) {
			Map<String, Object> legOut = new HashMap<>();

			/*if(hasTransferOnSameRoute(legs, leg)){
				throw new Exception("Transfer on same route");
			}
*/
			// Trip Info
			legOut.put("routeLongName", leg.routeLongName);
			legOut.put("routeId", leg.routeId);
			legOut.put("headsign", leg.headsign);
			legOut.put("tripShortName", leg.tripShortName);
			legOut.put("tripId", leg.tripId);
			legOut.put("direction", leg.tripDirectionId);
			legOut.put("destination", leg.stopHeadsign);
			legOut.put("from", leg.from.stopId);
			legOut.put("to", leg.to.stopId);
			legOut.put("stops", getStops(leg.stop, leg.from, leg.to));
			legOut.put("track", leg.from.track);
			legOut.put("stopNote", leg.from.note);
			legOut.put("occupancy", getOccupancy(leg.vehicleInfo));
			legOut.put("carriages", getCarriages(leg.vehicleInfo));
			legOut.put("alerts", leg.alerts);
			legOut.put("cancelled", isTripCancelled(leg.serviceDate, leg.tripId, cancelledTripsByDepartureTime));
			legOut.put("hold", isHeld(tripToRealTimeSignText, leg.tripId));

			// Date and Time Info
			legOut.put("runDate", leg.serviceDate);
			legOut.put("departTime", leg.startTime.getTime().getTime()/1000);
			legOut.put("departTimeString", leg.startTimeFmt);
			legOut.put("arriveTime", leg.endTime.getTime().getTime()/1000);
			legOut.put("arriveTimeString", leg.endTimeFmt);
			legOut.put("boardTime", leg.scheduledDepartureTimeFmt);
			legOut.put("alightTime", leg.scheduledArrivalTimeFmt);
			legOut.put("peak", leg.peakOffpeak);
			legOut.put("arrivalDelay", leg.arrivalDelay);
			legOut.put("departureDelay", leg.departureDelay);

			legs.add(legOut);
		}
		return legs;
	}

	private boolean hasTransferOnSameRoute(List<Map<String, Object>> legs, Leg currentLeg){
		if(!legs.isEmpty()){
			AgencyAndId prevRoute = (AgencyAndId) legs.stream().reduce((first, second) -> second).get().get("routeId");
			if(currentLeg.routeId.equals(prevRoute)){
				return true;
			}
		}
		return false;
	}


	private List<String> getStops(List<Place> stops, Place from, Place to){
		List<String> stopIds = new ArrayList<>();
		stopIds.add(AgencyAndId.convertToString(from.stopId));
		if(stops != null) {
			for (Place stop : stops) {
				stopIds.add(AgencyAndId.convertToString(stop.stopId));
			}
		}
		stopIds.add(AgencyAndId.convertToString(to.stopId));
		return stopIds;
	}

	private Boolean isTripCancelled(String serviceDate, AgencyAndId tripId,
									Map<String, Set<AgencyAndId>> cancelledTripsByDepartureTime) {
		if(serviceDate != null && cancelledTripsByDepartureTime.get(serviceDate) != null){
			return cancelledTripsByDepartureTime.get(serviceDate).contains(tripId);
		} else {
			for(Set<AgencyAndId> tripIds : cancelledTripsByDepartureTime.values()){
				if(tripIds.contains(tripId)){
					return true;
				}
			}
		}
		return false;
	}

	private String getOccupancy(VehicleInfo vehicleInfo) {
		if (vehicleInfo != null && vehicleInfo.getOccupancyStatus() != null){
			return vehicleInfo.getOccupancyStatus().name();
		}
		return null;
	}

	private List<CarriageInfo> getCarriages(VehicleInfo vehicleInfo) {
		if (vehicleInfo != null && vehicleInfo.getCarriages() != null){
			return vehicleInfo.getCarriages();
		}
		return null;
	}

	private boolean isHeld(Map<AgencyAndId, String> tripToRealTimeSignText, AgencyAndId tripId) {
		return tripToRealTimeSignText.get(tripId) != null
				&& tripToRealTimeSignText.get(tripId).toUpperCase().equals("HELD");
	}

	private Router getRouter(DataFetchingEnvironment environment) {
		return environment.<GraphQLRequestContext>getContext().getRouter();
	}
}