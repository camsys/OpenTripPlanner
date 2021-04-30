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
package org.opentripplanner.routing.error;

import org.junit.BeforeClass;
import org.junit.Test;
import org.opentripplanner.graph_builder.module.FakeGraph;
import org.opentripplanner.routing.algorithm.AStar;
import org.opentripplanner.routing.algorithm.strategies.InterleavedBidirectionalHeuristic;
import org.opentripplanner.routing.core.RoutingRequest;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.impl.DefaultStreetVertexIndexFactory;
import org.opentripplanner.routing.spt.ShortestPathTree;

import static org.junit.Assert.assertFalse;

public class EndpointsTooFarTest {

    private static final String NEAR_STOP_S1 = "40.22,-83.09";

    private static final String NEAR_STOP_S2 = "39.958,-83.012";

    private static final String MIDDLE_LOCATION_1 = "39.9908,-83.0118";

    private static final String MIDDLE_LOCATION_2 = "39.96383,-82.96291";

    private static final String FAR_LOCATION_1 = "02.9908,-15.0118";

    private static final String FAR_LOCATION_2 = "85.96383,-99.96291";

    private static Graph graph;

    @BeforeClass
    public static void setup() throws Exception {
        graph = FakeGraph.buildGraphNoTransit();
        FakeGraph.addTransit(graph);
        FakeGraph.link(graph);
        graph.index(new DefaultStreetVertexIndexFactory());
    }

    @Test
    public void testBothLocationsOk() {
        RoutingRequest opt = getOptions(NEAR_STOP_S1, NEAR_STOP_S2);
        AStar aStar = new AStar();
        ShortestPathTree spt = aStar.getShortestPathTree(opt, -1);
        assertFalse(spt.getPaths().isEmpty());
        opt.cleanup();
    }

    @Test(expected = OriginTooFarException.class)
    public void testOriginTooFar() {
        RoutingRequest opt = getOptions(FAR_LOCATION_1, NEAR_STOP_S2);
        AStar aStar = new AStar();
        aStar.getShortestPathTree(opt, -1);
        opt.cleanup();
    }

    @Test(expected = DestinationTooFarException.class)
    public void testDestinationTooFar() {
        RoutingRequest opt = getOptions(NEAR_STOP_S2, FAR_LOCATION_1);
        AStar aStar = new AStar();
        aStar.getShortestPathTree(opt, -1);
        opt.cleanup();
    }

    @Test(expected = BothEndpointsTooFarException.class)
    public void testBothTooFar() {
        RoutingRequest opt = getOptions(FAR_LOCATION_1, FAR_LOCATION_2);
        AStar aStar = new AStar();
        aStar.getShortestPathTree(opt, -1);
        opt.cleanup();
    }

    @Test
    public void testStopsAreOk() {
        String feedId = graph.index.feedInfoForId.keySet().iterator().next();
        RoutingRequest opt = getOptions(feedId + ":s1", feedId + ":s2");
        AStar aStar = new AStar();
        ShortestPathTree spt = aStar.getShortestPathTree(opt, -1);
        assertFalse(spt.getPaths().isEmpty());
        opt.cleanup();
    }

    @Test
    public void testKissAndRideBeforeOk() {
        RoutingRequest opt = getOptions(MIDDLE_LOCATION_1, NEAR_STOP_S2);
        opt.kissAndRide = true;
        AStar aStar = new AStar();
        ShortestPathTree spt = aStar.getShortestPathTree(opt, -1);
        assertFalse(spt.getPaths().isEmpty());
        opt.cleanup();
    }

    @Test
    public void testKissAndRideAfterOk() {
        RoutingRequest opt = getOptions(NEAR_STOP_S2, MIDDLE_LOCATION_1);
        opt.kissAndRide = true;
        opt.arriveBy = true;
        AStar aStar = new AStar();
        ShortestPathTree spt = aStar.getShortestPathTree(opt, -1);
        opt.cleanup();
    }

    private RoutingRequest getOptions(String origin, String destination) {
        RoutingRequest opt = new RoutingRequest();
        opt.setFromString(origin);
        opt.setToString(destination);
        opt.setRoutingContext(graph);
        opt.farEndpointsException = true;
        opt.maxWalkDistance = 1600.0;
        opt.rctx.remainingWeightHeuristic = new InterleavedBidirectionalHeuristic();
        return opt;
    }
}
