/*
 *  This program is free software: you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public License
 *  as published by the Free Software Foundation, either version 3 of
 *  the License, or (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.opentripplanner.routing.connectivity;

import org.opentripplanner.routing.alertpatch.Alert;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.edgetype.PathwayEdge;
import org.opentripplanner.routing.graph.Edge;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.routing.vertextype.TransitStop;

import java.util.*;

/**
 * Implementation of the template method pattern for traversing graphs within a station.
 * @param <T> Type of connectivity result to be returned by computeConnectivityResult.
 */
public abstract class ConnectivityTemplate <T> {

    protected Graph graph;

    public ConnectivityTemplate(Graph graph) {
        this.graph = graph;
    }

    /**
     * For the given initial graph, starting from tstop, using the date/time in state, traverse the graph
     * following only pathway edges that satisfy canUsePathway. Return if testForEarlyReturn is true, otherwise
     * return once all useable pathways have ben traversed.
     * @param state .
     * @param tstop .
     * @return object of type &lt;T&gt;
     */
    public T computeConnectivityResult(State state, TransitStop tstop) {
        Set<Vertex> seen = new HashSet<>();
        LinkedList<Vertex> queue = new LinkedList<>();
        Set<PathwayEdge> links = new HashSet<>();
        Set<Vertex> vAccessibles = new HashSet<>();
        Set<Edge> eAccessibles = new HashSet<>();

        queue.push(tstop);
        seen.add(tstop);
        List<Alert> alerts = new ArrayList<>();

        while (!queue.isEmpty()) {
            Vertex v = queue.pop();
            if (testForEarlyReturn(v)) {
               return buildResult(tstop, seen, vAccessibles, alerts, state, links, true);
            }
            if (v instanceof TransitStop &&
                    ((TransitStop) v).isEntrance() &&
                    ((TransitStop) v).hasWheelchairEntrance()) {
                vAccessibles.add(v);
            }
            for (Edge e : v.getOutgoing()) {
                if (e instanceof PathwayEdge) {
                    links.add((PathwayEdge) e);
                    if (canUsePathway(state, (PathwayEdge) e, alerts)) {
                        if (e.isWheelchairAccessible()) eAccessibles.add(e);
                        Vertex w = e.getToVertex();
                        if (!seen.contains(w)) {
                            seen.add(w);
                            queue.add(w);
                        }
                    }
                }
            }
        }
        queue.addAll(vAccessibles);
        while (!queue.isEmpty()) {
            Vertex v = queue.pop();
            for (Edge e : v.getOutgoing()) {
                if (eAccessibles.contains(e)) {
                    Vertex to = e.getToVertex();
                    if (!vAccessibles.contains(to)) {
                        vAccessibles.add(to);
                        queue.add(to);
                    }
                }
            }
        }
        return buildResult(tstop, seen, vAccessibles, alerts, state, links, false);
    }

    /**
     *
     * @param tstop .
     * @param vertices .
     * @param accessibles .
     * @param alerts .
     * @param state .
     * @param links .
     * @param earlyReturn .
     * @return object of type &lt;T&gt;
     */
    abstract protected T buildResult(TransitStop tstop, Set<Vertex> vertices, Set<Vertex> accessibles,
                                     List<Alert> alerts, State state, Set<PathwayEdge> links, boolean earlyReturn);

    /**
     *
     * @param v .
     * @return true or false
     */
    abstract protected boolean testForEarlyReturn(Vertex v);

    /**
     *
     * @param state .
     * @param pathway .
     * @param alerts .
     * @return return true or false
     */
    abstract protected boolean canUsePathway(State state, PathwayEdge pathway, List<Alert> alerts);

}
