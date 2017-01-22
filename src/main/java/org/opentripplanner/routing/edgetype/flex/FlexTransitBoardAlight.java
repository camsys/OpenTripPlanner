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

package org.opentripplanner.routing.edgetype.flex;

import org.opentripplanner.routing.core.RoutingRequest;
import org.opentripplanner.routing.core.TraverseMode;
import org.opentripplanner.routing.edgetype.PreBoardEdge;
import org.opentripplanner.routing.edgetype.TransitBoardAlight;
import org.opentripplanner.routing.edgetype.TripPattern;
import org.opentripplanner.routing.vertextype.PatternStopVertex;
import org.opentripplanner.routing.vertextype.flex.FlexDepartOnboard;
import org.opentripplanner.routing.vertextype.flex.FlexStopDepart;

public class FlexTransitBoardAlight extends TransitBoardAlight {

    private TripPattern pattern;

    public FlexTransitBoardAlight(FlexStopDepart from, FlexDepartOnboard to, TripPattern pattern) {
        super(from, to, from.getIndex(), pattern.mode, true);
        this.pattern = pattern;
    }

    // TODO: override stuff

    @Override
    public TripPattern getPattern() {
        return pattern;
    }
}
