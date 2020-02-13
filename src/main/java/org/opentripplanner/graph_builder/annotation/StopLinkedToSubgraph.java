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

package org.opentripplanner.graph_builder.annotation;

import org.opentripplanner.common.geometry.Subgraph;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.routing.vertextype.TransitStop;
import org.opentripplanner.routing.vertextype.TransitVertex;

public class StopLinkedToSubgraph extends GraphBuilderAnnotation {

    private static final long serialVersionUID = 1L;

    public static final String FMT = "Stop %s linked to subgraph with size %s";

    final TransitStop stop;
    final int size;

    public StopLinkedToSubgraph(TransitStop stop, Subgraph subgraph) {
        this.stop = stop;
        this.size = subgraph.streetSize();
    }

    @Override
    public String getMessage() {
        return String.format(FMT, stop, Integer.toString(size));
    }

    @Override
    public String getHTMLMessage() {
        return getMessage();
    }

    @Override
    public Vertex getReferencedVertex() {
        return this.stop;
    }
}