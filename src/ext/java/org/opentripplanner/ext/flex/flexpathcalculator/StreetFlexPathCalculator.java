package org.opentripplanner.ext.flex.flexpathcalculator;

import org.locationtech.jts.geom.LineString;
import org.onebusaway.gtfs.model.StopTime;
import org.opentripplanner.common.model.P2;
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
import org.opentripplanner.standalone.config.OtpConfig;

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

  private static long MAX_FLEX_TRIP_DURATION_SECONDS = Duration.ofHours(OtpConfig.flexMaxTripDurationInHours).toSeconds();

  private static long DEFAULT_FLEX_TRIP_DURATION_SECONDS = Duration.ofMinutes(90).toSeconds();

  private final Graph graph;

  private final Map<Vertex, ShortestPathTree> originDestinationCache = new HashMap<>();
  private final Map<Vertex, ShortestPathTree> originCache = new HashMap<>();

  private final boolean reverseDirection;

  private final boolean isDirectFlexOnly;

  public StreetFlexPathCalculator(Graph graph, boolean reverseDirection, boolean isDirectFlexOnly) {
    this.graph = graph;
    this.reverseDirection = reverseDirection;
    this.isDirectFlexOnly = isDirectFlexOnly;
  }

  // NB: duration can never be 0!
  @Override
  public FlexPath calculateFlexPath(Vertex fromv, Vertex tov, int fromStopIndex, int toStopIndex, FlexTrip trip) {

    // These are the origin and destination vertices from the perspective of the one-to-many search,
    // which may be reversed

    Vertex originVertex = reverseDirection ? tov : fromv;
    Vertex destinationVertex = reverseDirection ? fromv : tov;

    GraphPath path = null;

    if(!isDirectFlexOnly) {
      path = getOriginSTPPath(originVertex, destinationVertex);
    }

    if(path == null){
      path = getOriginDestinationSTPPath(originVertex, destinationVertex);
      if(path == null){
        return null;
      }
    }

    int distance = (int) path.getDistanceMeters();
    int duration = path.getDuration();

    return new FlexPath(distance, duration, path.getGeometry());
  }

  private GraphPath getOriginDestinationSTPPath(Vertex originVertex, Vertex destinationVertex){
    ShortestPathTree originDestinationShortestTreePath;
    if (originDestinationCache.containsKey(originVertex)) {
      originDestinationShortestTreePath = originDestinationCache.get(originVertex);
    } else {
      originDestinationShortestTreePath = routeToMany(originVertex, destinationVertex);
      originDestinationCache.put(originVertex, originDestinationShortestTreePath);
    }
    return originDestinationShortestTreePath.getPath(destinationVertex, false);
  }

  private GraphPath getOriginSTPPath(Vertex originVertex, Vertex destinationVertex){
    ShortestPathTree originShortestTreePath;
    Vertex key = originVertex;
    if (originCache.containsKey(key)) {
      originShortestTreePath = originCache.get(key);
    } else {
      originShortestTreePath = routeToMany(originVertex, null, true);
      originCache.put(key, originShortestTreePath);
    }
    return originShortestTreePath.getPath(destinationVertex, false);
  }

  private ShortestPathTree routeToMany(Vertex vertex, Vertex destinationVertex){
    return routeToMany(vertex, destinationVertex, false);
  }
  private ShortestPathTree routeToMany(Vertex vertex, Vertex destinationVertex, boolean useDefaultDuration) {

    RoutingRequest routingRequest = new RoutingRequest(TraverseMode.CAR);

    routingRequest.arriveBy = reverseDirection;

    if (reverseDirection) {
      routingRequest.setRoutingContext(graph, destinationVertex, vertex);
    } else {
      routingRequest.setRoutingContext(graph, vertex, destinationVertex);
    }

    routingRequest.disableRemainingWeightHeuristic = true;
    routingRequest.rctx.remainingWeightHeuristic = new TrivialRemainingWeightHeuristic();
    routingRequest.dominanceFunction = new DominanceFunction.EarliestArrival();
    routingRequest.oneToMany = true;

    AStar search = new AStar();
    if(useDefaultDuration){
      search.setSkipEdgeStrategy(new DurationSkipEdgeStrategy(DEFAULT_FLEX_TRIP_DURATION_SECONDS));
    } else {
      search.setSkipEdgeStrategy(new DurationSkipEdgeStrategy(MAX_FLEX_TRIP_DURATION_SECONDS));
    }


    ShortestPathTree spt = search.getShortestPathTree(routingRequest);
    routingRequest.cleanup();

    return spt;
  }
}
