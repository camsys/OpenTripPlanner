/* This program is free software: you can redistribute it and/or
 modify it under the terms of the GNU Lesser General Public License
 as published by the Free Software Foundation, either version 3 of
 the License, or (props, at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>. */

package org.opentripplanner.routing.algorithm.strategies;

import gnu.trove.map.TObjectDoubleMap;
import gnu.trove.map.hash.TObjectDoubleHashMap;
import org.onebusaway.gtfs.model.Route;
import org.opentripplanner.common.pqueue.BinHeap;
import org.opentripplanner.routing.core.RoutingRequest;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.core.TraverseMode;
import org.opentripplanner.routing.edgetype.PathwayEdge;
import org.opentripplanner.routing.edgetype.StreetTransitLink;
import org.opentripplanner.routing.error.BothEndpointsTooFarException;
import org.opentripplanner.routing.error.DestinationTooFarException;
import org.opentripplanner.routing.error.OriginTooFarException;
import org.opentripplanner.routing.graph.Edge;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.routing.location.StreetLocation;
import org.opentripplanner.routing.spt.DominanceFunction;
import org.opentripplanner.routing.spt.ShortestPathTree;
import org.opentripplanner.routing.vertextype.StreetVertex;
import org.opentripplanner.routing.vertextype.TransitStop;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

/**
 * This the goal direction heuristic used for transit searches.
 *
 * Euclidean heuristics are terrible for transit routing because the maximum transit speed is quite high, especially
 * relative to the walk speed. Transit can require going away from the destination in Euclidean space to approach it
 * according to the travel time metric. This heuristic is designed to be good for transit.
 *
 * After many experiments storing travel time metrics in tables or embedding them in low-dimensional Euclidean space I
 * eventually came to the conclusion that the most efficient structure for representing the metric was already right
 * in front of us: a graph.
 *
 * This heuristic searches backward from the target vertex over the street and transit network, removing any
 * time-dependent component of the network (e.g. by evaluating all boarding wait times as zero). This produces an
 * admissible heuristic (i.e. it always underestimates path weight) making it valid independent of the clock time.
 * This is important because you don't know precisely what time you will arrive at the destination until you get there.
 *
 * Because we often make use of the first path we find in the main search, this heuristic must be both admissible and
 * consistent (monotonic). If the heuristic is non-monotonic, nodes can be re-discovered and paths are not necessarily
 * discovered in order of increasing weight. When finding paths one by one and banning trips or routes,
 * suboptimal paths may be found and reported before or instead of optimal ones.
 *
 * This heuristic was previously not consistent for the reasons discussed in ticket #2153. It was possible for the
 * "zero zone" around the origin to overlap the egress zone around the destination, leading to decreases in the
 * heuristic across an edge that were greater in magnitude than the weight of that edge. This has been solved by
 * creating two separate distance maps, one pre-transit and one post-transit.
 *
 * Note that the backward search does not happen in a separate thread. It is interleaved with the main search in a
 * ratio of N:1 iterations.
 */
public class InterleavedBidirectionalHeuristic implements RemainingWeightHeuristic {

    private static final long serialVersionUID = 20160215L;

    private static Logger LOG = LoggerFactory.getLogger(InterleavedBidirectionalHeuristic.class);

    // For each step in the main search, how many steps should the reverse search proceed?
    private static final int HEURISTIC_STEPS_PER_MAIN_STEP = 8; // TODO determine a good value empirically

    private static final int OVERRIDE_THRESHOLD = 1608; // one mile

    /** The vertex at which the main search begins. */
    Vertex origin;

    /** The vertex that the main search is working towards. */
    private Vertex target;

    /** Or search works towards multiple targets (landmark support) */
    private List<Vertex> targets;

    /** All vertices within walking distance of the origin (the vertex at which the main search begins). */
    Set<Vertex> preTransitVertices;

    /**
     * A lower bound on the weight of the lowest-cost path to the target (the vertex at which the main search ends)
     * from each vertex within walking distance of the target. As the heuristic progressively improves, this map will
     * include lower bounds on path weights for an increasing number of vertices on board transit.
     */
    TObjectDoubleMap<Vertex> postBoardingWeights;

    Graph graph;

    RoutingRequest routingRequest;

    // The maximum weight yet seen at a closed node in the reverse search. The priority queue head has a uniformly
    // increasing weight, so any unreached transit node must have greater weight than this.
    double maxWeightSeen = 0;

    // The priority queue for the interleaved backward search through the transit network.
    BinHeap<Vertex> transitQueue;

    // True when the entire transit network has been explored by the reverse search.
    boolean finished = false;

    // Keep track of "pre-transit" transit stops so we can check for kiss-and-ride access
    private BinHeap<TransitStop> preTransitStopsByDistance;

    private BinHeap<TransitStop> postTransitStopByDistance;

    private boolean postTransitKissAndRide = false;

    // save if we do kiss-and-ride
    private BinHeap<Vertex> transitQueue_WALK;
    private double maxWeightSeen_WALK;
    private TObjectDoubleMap<Vertex> postBoardingWeights_WALK;
    private Set<TransitStop> kissAndRideStopsToFind = new HashSet<>();

    /**
     * Before the main search begins, the heuristic must search on the streets around the origin and destination.
     * This also sets up the initial states for the reverse search through the transit network, which progressively
     * improves lower bounds on travel time to the target to guide the main search.
     *
     */
    @Override
    public void initialize(RoutingRequest request, long abortTime) {
        // This code used to use the RoutingRequest for the original search, & as a side effect,
        // set softWalkLimiting = false and maxWalkDistance = Infinity. Now, we use a cloned version of the
        // request instead.
        Vertex target = request.rctx.target;
        List<Vertex> targets = request.rctx.targets;
        if ((target != null && target == this.target || (targets != null && targets == this.targets))) {
            if (request.postTransitKissAndRide != this.postTransitKissAndRide) {
                transitQueue = transitQueue_WALK;
                maxWeightSeen = maxWeightSeen_WALK;
                postBoardingWeights = postBoardingWeights_WALK;
                this.postTransitKissAndRide = false;
            }
            LOG.debug("Reusing existing heuristic, the target vertex has not changed.");
            return;
        }
        LOG.debug("Initializing heuristic computation.");
        this.graph = request.rctx.graph;
        long start = System.currentTimeMillis();
        this.target = target;
        this.targets = targets;
        this.routingRequest = request.clone();
        routingRequest.excludeWalking = false;
        routingRequest.softWalkLimiting = false;
        routingRequest.softPreTransitLimiting = false;
        routingRequest.maxWalkDistance = request.maxWalkDistanceHeuristic;
        transitQueue = new BinHeap<>();
        maxWeightSeen = 0;
        preTransitStopsByDistance = new BinHeap<>();
        postTransitStopByDistance = new BinHeap<>();
        // Forward street search first, mark street vertices around the origin so H evaluates to 0
        TObjectDoubleMap<Vertex> forwardStreetSearchResults = streetSearch(routingRequest, false, abortTime, false);
        if (forwardStreetSearchResults == null) {
            return; // Search timed out
        }
        preTransitVertices = forwardStreetSearchResults.keySet();
        LOG.debug("end forward street search {} ms", System.currentTimeMillis() - start);
        postBoardingWeights = streetSearch(routingRequest, true, abortTime, false);
        if (postBoardingWeights == null) {
            return; // Search timed out
        }
        LOG.debug("end backward street search {} ms", System.currentTimeMillis() - start);
        LOG.debug("initialized SSSP");
        if (request.smartKissAndRide) {
            modifyForSmartKissAndRide(request);
            if (request.postTransitKissAndRide) {
                // save old data structures
                transitQueue_WALK = transitQueue;
                maxWeightSeen_WALK = maxWeightSeen;
                postBoardingWeights_WALK = postBoardingWeights;
                // redo transit queue
                transitQueue = new BinHeap<>();
                maxWeightSeen = 0;
                postBoardingWeights = streetSearch(request, true, abortTime, true);
                postTransitKissAndRide = true;
            }
        }
        request.rctx.debugOutput.finishedPrecalculating();
        checkEndpoints();
    }

    /**
     * This function supplies the main search with an (under)estimate of the remaining path weight to the target.
     * No matter how much progress has been made on the reverse heuristic search, we must return an underestimate
     * of the cost to reach the target (i.e. the heuristic must be admissible).
     * All on-street vertices within walking distance of the origin or destination will have been explored by the
     * heuristic before the main search starts.
     */
    @Override
    public double estimateRemainingWeight (State s) {
        final Vertex v = s.getVertex();
        if (v instanceof StreetLocation) {
            // Temporary vertices (StreetLocations) might not be found in the street searches.
            // Zero is always an underestimate.
            return 0;
        }
        if (v instanceof StreetVertex) {
            // The main search is on the streets, not on transit.
            if (s.isEverBoarded()) {
                // If we have already ridden transit we must be near the destination. If not the map returns INF.
                return postBoardingWeights.get(v);
            } else {
                // We have not boarded transit yet. We have no idea what the weight to the target is so return zero.
                // We could also use a Euclidean heuristic here.
                if (preTransitVertices.contains(v)) {
                    return 0;
                } else {
                    return Double.POSITIVE_INFINITY;
                }
            }
        } else {
            // The main search is not currently on a street vertex, it's probably on transit.
            // If the current part of the transit network has been explored, then return the stored lower bound.
            // Otherwise return the highest lower bound yet seen -- this location must have a higher cost than that.
            double h = postBoardingWeights.get(v);
            if (h == Double.POSITIVE_INFINITY) {
                return maxWeightSeen;
            } else {
                return h;
            }
        }
    }

    @Override
    public void reset() { }

    /**
     * Move backward N steps through the transit network.
     * This improves the heuristic's knowledge of the transit network as seen from the target,
     * making its lower bounds on path weight progressively more accurate.
     */
    @Override
    public void doSomeWork() {
        if (finished) return;
        for (int i = 0; i < HEURISTIC_STEPS_PER_MAIN_STEP; ++i) {
            if (transitQueue.empty()) {
                finished = true;
                break;
            }
            int uWeight = (int) transitQueue.peek_min_key();
            Vertex u = transitQueue.extract_min();
            // The weight of the queue head is uniformly increasing.
            // This is the highest weight ever seen for a closed vertex.
            maxWeightSeen = uWeight;
            // Now that this vertex is closed, we can store its weight for use as a lower bound / heuristic value.
            // We don't implement decrease-key operations though, so check whether a smaller value is already known.
            double uWeightOld = postBoardingWeights.get(u);
            if (uWeight < uWeightOld) {
                // Including when uWeightOld is infinite because the vertex is not yet closed.
                postBoardingWeights.put(u, uWeight);
            } else {
                // The vertex was already closed. This time it necessarily has a higher weight, so skip it.
                continue;
            }
            // This search is proceeding backward relative to the main search.
            // When the main search is arriveBy the heuristic search looks at OUTgoing edges.
            for (Edge e : routingRequest.arriveBy ? u.getOutgoing() : u.getIncoming()) {
                // Do not enter streets in this phase, which should only touch transit.
                if (e instanceof StreetTransitLink) {
                    continue;
                }
                Vertex v = routingRequest.arriveBy ? e.getToVertex() : e.getFromVertex();
                double edgeWeight = e.weightLowerBound(routingRequest);
                // INF heuristic value indicates unreachable (e.g. non-running transit service)
                // this saves time by not reverse-exploring those routes and avoids maxFound of INF.
                if (Double.isInfinite(edgeWeight)) {
                    continue;
                }
                double vWeight = uWeight + edgeWeight;
                double vWeightOld = postBoardingWeights.get(v);
                if (vWeight < vWeightOld) {
                    // Should only happen when vWeightOld is infinite because it is not yet closed.
                    transitQueue.insert(v, vWeight);
                }
            }
        }
    }

    /**
     * Explore the streets around the origin or target, recording the minimum weight of a path to each street vertex.
     * When searching around the target, also retain the states that reach transit stops since we'll want to
     * explore the transit network backward, in order to guide the main forward search.
     *
     * The main search always proceeds from the "origin" to the "target" (names remain unchanged in arriveBy mode).
     * The reverse heuristic search always proceeds outward from the target (name remains unchanged in arriveBy).
     *
     * When the main search is departAfter:
     * it gets outgoing edges and traverses them with arriveBy=false,
     * the heuristic search gets incoming edges and traverses them with arriveBy=true,
     * the heuristic destination street search also gets incoming edges and traverses them with arriveBy=true,
     * the heuristic origin street search gets outgoing edges and traverses them with arriveBy=false.
     *
     * When main search is arriveBy:
     * it gets incoming edges and traverses them with arriveBy=true,
     * the heuristic search gets outgoing edges and traverses them with arriveBy=false,
     * the heuristic destination street search also gets outgoing edges and traverses them with arriveBy=false,
     * the heuristic origin street search gets incoming edges and traverses them with arriveBy=true.
     * The streetSearch method traverses using the real traverse method rather than the lower bound traverse method
     * because this allows us to keep track of the distance walked.
     * Perhaps rather than tracking walk distance, we should just check the straight-line radius and
     * only walk within that distance. This would avoid needing to call the main traversal functions.
     *
     * TODO what if the egress segment is by bicycle or car mode? This is no longer admissible.
     */
    private TObjectDoubleMap<Vertex> streetSearch (RoutingRequest rr, boolean fromTarget, long abortTime, boolean kissAndRide) {
        LOG.debug("Heuristic street search around the {}.", fromTarget ? "target" : "origin");
        rr = rr.clone();
        rr.softWalkLimiting = true;
        if (fromTarget) {
            rr.setArriveBy(!rr.arriveBy);
        }
        if (kissAndRide) {
            rr.preTransitKissAndRide = true;
            rr.postTransitKissAndRide = false;
        }
        // Create a map that returns Infinity when it does not contain a vertex.
        TObjectDoubleMap<Vertex> vertices = new TObjectDoubleHashMap<>(100, 0.5f, Double.POSITIVE_INFINITY);
        ShortestPathTree spt = new DominanceFunction.MinimumWeight().getNewShortestPathTree(rr);
        // TODO use normal OTP search for this.
        BinHeap<State> pq = new BinHeap<State>();
        Vertex initVertex = fromTarget ? rr.rctx.target : rr.rctx.origin;
        if (initVertex != null) {
            State initState = new State(initVertex, rr);
            pq.insert(initState, 0);
        }

        if(fromTarget) {
            // Add all the targets for trips with multiple possible targets.
            for (int i = 0; i < rr.rctx.targets.size(); i++) {
                Vertex anotherVertex = rr.rctx.targets.get(i);
                State anotherState = new State(anotherVertex, rr);
                pq.insert(anotherState, 0);
            }
        }
        else{
            // Add all the origins for trips with multiple possible targets.
            for (int i = 0; i < rr.rctx.origins.size(); i++) {
                Vertex anotherVertex = rr.rctx.origins.get(i);
                State anotherState = new State(anotherVertex, rr);
                pq.insert(anotherState, 0);
            }
        }

        // override is irrelevant if not defined in request
        boolean foundOverrideStop = routingRequest.kissAndRideOverrides.isEmpty();

        while ( ! pq.empty()) {
            //TODO RTD Flex remove
            boolean shouldAbort = false;
            if (abortTime < Long.MAX_VALUE  && System.currentTimeMillis() > abortTime && shouldAbort) {
                return null;
            }
            State s = pq.extract_min();
            Vertex v = s.getVertex();
            // At this point the vertex is closed (pulled off heap).
            // This is the lowest cost we will ever see for this vertex. We can record the cost to reach it.
            if (v instanceof TransitStop) {
                // We don't want to continue into the transit network yet, but when searching around the target
                // place vertices on the transit queue so we can explore the transit network backward later.
                TransitStop tstop = (TransitStop) v;
                if (stopIsBanned(tstop)) {
                    continue;
                }
                if (fromTarget) {
                    double weight = s.getWeight();
                    transitQueue.insert(v, weight);
                    if (weight > maxWeightSeen) {
                        maxWeightSeen = weight;
                    }
                    postTransitStopByDistance.insert(tstop, s.getWalkDistance());
                } else {
                    preTransitStopsByDistance.insert(tstop, s.getWalkDistance());
                }
                String feedId = tstop.getStopId().getAgencyId();
                foundOverrideStop |= routingRequest.kissAndRideOverrides.contains(feedId);
                continue;
            }
            // We don't test whether we're on an instanceof StreetVertex here because some other vertex types
            // (park and ride or bike rental related) that should also be explored and marked as usable.
            // Record the cost to reach this vertex.
            if (!vertices.containsKey(v)) {
                vertices.put(v, (int) s.getWeight()); // FIXME time or weight? is RR using right mode?
            }
            for (Edge e : rr.arriveBy ? v.getIncoming() : v.getOutgoing()) {
                // arriveBy has been set to match actual directional behavior in this subsearch.
                for (State s1 = e.traverse(s); s1 != null; s1 = s1.getNextResult()) {
                    if (searchShouldTerminate(rr, s1, foundOverrideStop, kissAndRide)) {
                        continue;
                    }
                    if (spt.add(s1)) {
                        pq.insert(s1, s1.getWeight());
                    }
                }
            }
        }
        LOG.debug("Heuristic street search hit {} vertices.", vertices.size());
        LOG.debug("Heuristic street search hit {} transit stops.", transitQueue.size());
        return vertices;
    }

    private boolean searchShouldTerminate(RoutingRequest opt, State s1, boolean foundOverrideStop, boolean kissAndRide) {
        if (kissAndRide) {
            return transitQueue.getElements().containsAll(kissAndRideStopsToFind);
        } else {
            return foundOverrideStop && s1.getWalkDistance() > opt.maxWalkDistanceHeuristic;
        }
    }

    private void modifyForSmartKissAndRide(RoutingRequest request) {
        if (shouldUseKissAndRide(preTransitStopsByDistance, request.kissAndRideOverrides, false)) {
            request.preTransitKissAndRide = true;
            LOG.debug("should use pre-transit k&r");
        }
        if (shouldUseKissAndRide(postTransitStopByDistance, request.kissAndRideOverrides, true)) {
            request.postTransitKissAndRide = true;
            LOG.debug("should use post-transit k&r, stops to find: " + kissAndRideStopsToFind);
        }
        request.smartKissAndRide = request.preTransitKissAndRide || request.postTransitKissAndRide;
    }

    private boolean shouldUseKissAndRide(BinHeap<TransitStop> heap,  Set<String> feedOverrides, boolean fromTarget) {
        boolean exceedThreshold = false;
        boolean foundWhitelistedStop = false;
        while (!heap.empty()) {
            double distance = heap.peek_min_key();
            TransitStop stop = heap.extract_min();
            exceedThreshold = distance > OVERRIDE_THRESHOLD;
            if (!exceedThreshold && feedOverrides.contains(stop.getStopId().getAgencyId())) {
                return false;
            }
            if (routingRequest.canUseStopForKissAndRide(stop.getStop())) {
                foundWhitelistedStop |= true;
                if (fromTarget)
                    kissAndRideStopsToFind.add(stop);
            }
            if (exceedThreshold && foundWhitelistedStop) {
                return true;
            }
        }
        return false;
    }

    // For now just checks route_type. TODO agency, mode.
    // This is necessary for the smart-kiss-and-ride checks
    private boolean stopIsBanned(TransitStop tstop) {
        if (routingRequest.bannedRouteTypes.isEmpty())
            return false;
        if (!tstop.isEntrance() && !tstop.isExtendedLocationType())
            return routingRequest.bannedRouteTypes.containsAll(getRouteTypes(tstop));

        // Find all "actual" TransitStops via Pathways
        Set<TransitStop> seen = new HashSet<>();
        LinkedList<TransitStop> queue = new LinkedList<>();
        queue.push(tstop);
        seen.add(tstop);
        while (!queue.isEmpty()) {
            TransitStop v = queue.pop();
            if (!v.isEntrance() && !v.isExtendedLocationType()) {
                if (!routingRequest.bannedRouteTypes.containsAll(getRouteTypes(v)))
                    return false;
            }
            for (Edge e : v.getOutgoing()) {
                if (e instanceof PathwayEdge) {
                    TransitStop w = (TransitStop) e.getToVertex();
                    if (!seen.contains(w)) {
                        seen.add(w);
                        queue.add(w);
                    }
                }
            }
        }
        return true;
    }

    private Collection<Integer> getRouteTypes(TransitStop tstop) {
        Set<Integer> routeTypes = new HashSet<>();
        for (Route route : routingRequest.rctx.graph.index.routesForStop(tstop.getStop())) {
            routeTypes.add(route.getType());
        }
        return routeTypes;
    }

    private void checkEndpoints() {
        if (!routingRequest.farEndpointsException || routingRequest.kissAndRide || routingRequest.parkAndRide)
            return;
        boolean preTransitFar = preTransitStopsByDistance.empty()
                || preTransitStopsByDistance.peek_min_key() > routingRequest.maxWalkDistance;
        boolean postTransitFar = postTransitStopByDistance.empty()
                || postTransitStopByDistance.peek_min_key() > routingRequest.maxWalkDistance;
        if (preTransitFar && postTransitFar)
            throw new BothEndpointsTooFarException();
        else if (preTransitFar)
            throw new OriginTooFarException();
        else if (postTransitFar)
            throw new DestinationTooFarException();
    }
}
