package org.opentripplanner.ext.flex.flexpathcalculator;

import org.locationtech.jts.geom.LineString;
import org.onebusaway.gtfs.model.StopTime;
import org.opentripplanner.ext.flex.trip.FlexTrip;
import org.opentripplanner.ext.flex.trip.FlexTripStopTime;
import org.opentripplanner.ext.flex.trip.ScheduledDeviatedTrip;
import org.opentripplanner.routing.algorithm.astar.AStar;
import org.opentripplanner.routing.algorithm.astar.strategies.DurationSkipEdgeStrategy;
import org.opentripplanner.routing.algorithm.astar.strategies.TrivialRemainingWeightHeuristic;
import org.opentripplanner.routing.api.request.RoutingRequest;
import org.opentripplanner.routing.core.TraverseMode;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.routing.spt.DominanceFunction;
import org.opentripplanner.routing.spt.GraphPath;
import org.opentripplanner.routing.spt.ShortestPathTree;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * StreetFlexPathCalculator calculates the driving times and distances based on the street network
 * using the AStar algorithm.
 *
 * Note that it caches the whole ShortestPathTree the first time it encounters a new fromVertex.
 * Subsequents requests from the same fromVertex can fetch the path to the toVertex from the
 * existing ShortestPathTree. This one-to-many approach is needed to make the performance acceptable.
 *
 * Because we will have lots of searches with the same origin when doing access searches and a lot
 * of searches with the same destination when doing egress searches, the calculator needs to be
 * configured so that the caching is done with either the origin or destination vertex as the key.
 * The one-to-many search will then either be done in the forward or the reverse direction depending
 * on this configuration.
 */
public class StreetFlexPathCalculator implements FlexPathCalculator {

  private static final long MAX_FLEX_TRIP_DURATION_SECONDS = Duration.ofMinutes(45).toSeconds();

  private final Graph graph;

  private final Map<Vertex, ShortestPathTree> cache = new HashMap<>();
  
  private final boolean reverseDirection;

  public StreetFlexPathCalculator(Graph graph, boolean reverseDirection) {
    this.graph = graph;
    this.reverseDirection = reverseDirection;
  }

  // NB: duration can never be 0! 
  @Override
  public FlexPath calculateFlexPath(Vertex fromv, Vertex tov, int fromStopIndex, int toStopIndex, FlexTrip trip) {

	// These are the origin and destination vertices from the perspective of the one-to-many search,
    // which may be reversed
    Vertex originVertex = reverseDirection ? tov : fromv;
    Vertex destinationVertex = reverseDirection ? fromv : tov;

    ShortestPathTree shortestPathTree;
    if (cache.containsKey(originVertex)) {
      shortestPathTree = cache.get(originVertex);
    } else {
      shortestPathTree = routeToMany(originVertex);
      cache.put(originVertex, shortestPathTree);
    }

    GraphPath path = shortestPathTree.getPath(destinationVertex, false);
    if(path == null)
    	return null;
    
    int distance = (int) path.getDistanceMeters();
    int duration = path.getDuration();

    if(trip instanceof ScheduledDeviatedTrip) {
    	FlexTripStopTime fromST = trip.getStopTime(fromStopIndex);
    	FlexTripStopTime toST = trip.getStopTime(toStopIndex);

    	int newFromST = 0;
    	int newToST = 0;
    	
    	if(fromST.departureTime != StopTime.MISSING_VALUE)
    		newFromST = fromST.departureTime;
    	else
    		newFromST = fromST.flexWindowEnd - ((fromST.flexWindowEnd - fromST.flexWindowStart) / 2);

    	if(toST.arrivalTime != StopTime.MISSING_VALUE)
    		newToST = toST.arrivalTime;
    	else
    		newToST = toST.flexWindowStart + ((toST.flexWindowEnd - toST.flexWindowStart) / 2);

    	int newDuration = newToST - newFromST;    	
    	if(newDuration > 0) // filter invalid GTFS data; in this case, use street traversal time
    		duration = newDuration;
    }
    
    return new FlexPath(distance, duration, path.getGeometry());
  }

  private ShortestPathTree routeToMany(Vertex vertex) {
    RoutingRequest routingRequest = new RoutingRequest(TraverseMode.CAR);

    routingRequest.arriveBy = reverseDirection;
    
    if (reverseDirection) {
      routingRequest.setRoutingContext(graph, null, vertex);
    } else {
      routingRequest.setRoutingContext(graph, vertex, null);
    }
    
    routingRequest.disableRemainingWeightHeuristic = true;
    routingRequest.rctx.remainingWeightHeuristic = new TrivialRemainingWeightHeuristic();
    routingRequest.dominanceFunction = new DominanceFunction.EarliestArrival();
    routingRequest.oneToMany = true;
    
    AStar search = new AStar();
    search.setSkipEdgeStrategy(new DurationSkipEdgeStrategy(MAX_FLEX_TRIP_DURATION_SECONDS));
    
    ShortestPathTree spt = search.getShortestPathTree(routingRequest);
    routingRequest.cleanup();
    
    return spt;
  }
}
