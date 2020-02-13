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
package org.opentripplanner.routing.accessibility;

import org.opentripplanner.routing.alertpatch.Alert;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.edgetype.PathwayEdge;
import org.opentripplanner.routing.graph.Edge;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.routing.vertextype.TransitStop;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

public class MTAStopAccessibilityStrategy extends DefaultStopAccessibilityStrategy {

    private Graph graph;

    public MTAStopAccessibilityStrategy(Graph graph) {
        this.graph = graph;
    }

    @Override
    public AccessibilityResult stopIsAccessible(State state, TransitStop stop) {
        if (stop.getStopId().getAgencyId().equals("MTASBWY"))
            return stopIsAccessibleFromStreet(state, stop);
        return super.stopIsAccessible(state, stop);
    }

    private AccessibilityResult stopIsAccessibleFromStreet(State state, TransitStop tstop) {
        // only by traversing pathways, can we get to the street?
        Set<Vertex> seen = new HashSet<>();
        LinkedList<Vertex> queue = new LinkedList<>();
        queue.push(tstop);
        seen.add(tstop);
        List<Alert> alerts = new ArrayList<>();
        while (!queue.isEmpty()) {
            Vertex v = queue.pop();
            if (v instanceof TransitStop
                    && ((TransitStop) v).isEntrance() && ((TransitStop) v).hasWheelchairEntrance()) {
                // success
                return AccessibilityResult.ALWAYS_ACCESSIBLE;
            }
            for (Edge e : v.getOutgoing()) {
                if (e instanceof PathwayEdge && canUsePathway(state, (PathwayEdge) e, alerts)) {
                    Vertex w = e.getToVertex();
                    if (!seen.contains(w)) {
                        seen.add(w);
                        queue.add(w);
                    }
                }
            }
        }
        return AccessibilityResult.notAccessibleForReason(alerts);
    }

    private boolean canUsePathway(State state, PathwayEdge pathway, List<Alert> alerts) {
        if (!pathway.isWheelchairAccessible())
            return false;
        if (!pathway.isElevator())
            return true;
        if (state.getOptions().ignoreRealtimeUpdates)
            return true;
        List<Alert> newAlerts = pathway.getElevatorIsOutOfServiceAlerts(graph, state);
        alerts.addAll(newAlerts);
        return newAlerts.isEmpty();
    }
}
