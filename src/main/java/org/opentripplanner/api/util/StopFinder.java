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
package org.opentripplanner.api.util;

import org.opentripplanner.routing.algorithm.TraverseVisitor;
import org.opentripplanner.routing.algorithm.strategies.SearchTerminationStrategy;
import org.opentripplanner.routing.core.RoutingRequest;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.graph.Edge;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.routing.spt.ShortestPathTree;
import org.opentripplanner.routing.vertextype.TransitStop;

import java.util.HashMap;
import java.util.Map;

public class StopFinder implements TraverseVisitor, SearchTerminationStrategy {

    private double radius;

    private int minStops;

    private int maxStops;

    private Map<TransitStop, State> transitStopStates = new HashMap<>();

    private double distSearched = 0;

    public StopFinder(double radius, int minStops, int maxStops) {
        this.radius = radius;
        this.minStops = minStops;
        this.maxStops = maxStops;
    }

    @Override
    public void visitEdge(Edge edge, State state) {
    }

    @Override
    public void visitVertex(State state) {
        distSearched = Math.max(distSearched, state.getWalkDistance());
        if (state.getVertex() instanceof TransitStop) {
            TransitStop tstop = (TransitStop) state.getVertex();
            if (!tstop.isEntrance() && !tstop.isExtendedLocationType()) {
                transitStopStates.put(tstop, state);
            }
        }
    }

    @Override
    public void visitEnqueue(State state) {
    }

    @Override
    public boolean shouldSearchTerminate(Vertex origin, Vertex target, State current, ShortestPathTree spt, RoutingRequest traverseOptions) {
        if (transitStopStates.size() >= maxStops) {
            return true;
        }
        if (distSearched > radius && transitStopStates.size() >= minStops) {
            return true;
        }
        return false;
    }

    public Map<TransitStop, State> getStops() {
        return transitStopStates;
    }
}
