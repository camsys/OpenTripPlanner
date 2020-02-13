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

import org.onebusaway.gtfs.model.AgencyAndId;
import org.opentripplanner.routing.algorithm.TraverseVisitor;
import org.opentripplanner.routing.algorithm.strategies.SearchTerminationStrategy;
import org.opentripplanner.routing.algorithm.strategies.SkipTraverseResultStrategy;
import org.opentripplanner.routing.core.RoutingRequest;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.core.TraverseMode;
import org.opentripplanner.routing.graph.Edge;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.routing.spt.ShortestPathTree;
import org.opentripplanner.routing.vertextype.TransitStop;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class StopFinder implements TraverseVisitor, SearchTerminationStrategy {

    private double radius;

    private int minStops;

    private int maxStops;

    private Map<TransitStop, State> transitStopStates = new HashMap<>();

    private double distSearched = 0;

    private Set<AgencyAndId> stops = new HashSet<>();

    private boolean groupByParent;

    private Set<TraverseMode> modes;

    public StopFinder(double radius, int minStops, int maxStops, boolean groupByParent, Set<TraverseMode> modes) {
        this.radius = radius;
        this.minStops = minStops;
        this.maxStops = maxStops;
        this.groupByParent = groupByParent;
        this.modes = modes;
    }

    @Override
    public void visitEdge(Edge edge, State state) {
    }

    @Override
    public void visitVertex(State state) {
        distSearched = Math.max(distSearched, state.getWalkDistance());
        if (state.getVertex() instanceof TransitStop) {
            TransitStop tstop = (TransitStop) state.getVertex();
            if (!tstop.isEntrance() && !tstop.isExtendedLocationType() && hasMode(tstop)) {
                transitStopStates.put(tstop, state);

                // for termination condition, could be a parent stop
                AgencyAndId stopId = tstop.getStopId();
                if (groupByParent && tstop.getStop().getParentStation() != null) {
                    stopId = new AgencyAndId(stopId.getAgencyId(), tstop.getStop().getParentStation());
                }
                stops.add(stopId);
            }
        }
    }

    @Override
    public void visitEnqueue(State state) {
    }

    @Override
    public boolean shouldSearchTerminate(Vertex origin, Vertex target, State current, ShortestPathTree spt, RoutingRequest traverseOptions) {
        if (stops.size() >= maxStops) {
            return true;
        }
        if (distSearched > radius && stops.size() >= minStops) {
            return true;
        }
        return false;
    }

    public Map<TransitStop, State> getStops() {
        return transitStopStates;
    }

    private boolean hasMode(TransitStop tstop) {
        if (modes == null)
            return true;
        for (TraverseMode mode : modes) {
            if (tstop.getModes().contains(mode)) {
                return true;
            }
        }
        return false;
    }
}
