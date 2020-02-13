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
package org.opentripplanner.routing.transfers;

import org.onebusaway.gtfs.model.Stop;
import org.onebusaway.gtfs.model.Trip;
import org.opentripplanner.routing.accessibility.StopAccessibilityStrategy;
import org.opentripplanner.routing.core.RoutingRequest;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.core.StopTransfer;
import org.opentripplanner.routing.core.TransferTable;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.vertextype.TransitStop;


public class MTATransferPermissionStrategy implements TransferPermissionStrategy {

    private TransferTable transferTable;

    private Graph graph;

    private StopAccessibilityStrategy stopAccessibilityStrategy;

    public MTATransferPermissionStrategy(Graph graph) {
        this.graph = graph;
        transferTable = graph.getTransferTable();
        stopAccessibilityStrategy = graph.stopAccessibilityStrategy;
    }

    @Override
    public boolean isTransferAllowed(State state, Stop fromStop, Stop toStop, Trip fromTrip, Trip toTrip, boolean boarding, int transferTime) {
        RoutingRequest options = state.getOptions();
        String fromFeed = fromStop.getId().getAgencyId();
        String toFeed = toStop.getId().getAgencyId();
        if (transferTime == StopTransfer.UNKNOWN_TRANSFER && !options.allowUnknownTransfers && transferTable.hasFeedTransfers(fromFeed, toFeed, boarding)) {
            return false;
        }

        // Don't apply transfer rules unless both stops are MTASBWY stops.
        if (fromStop.getId().getAgencyId().equals("MTASBWY") && toStop.getId().getAgencyId().equals("MTASBWY")) {
            if (options.wheelchairAccessible && !fromStop.equals(toStop)) {
                TransitStop fromTransitStop = graph.index.stopVertexForStop.get(fromStop);
                TransitStop toTransitStop = graph.index.stopVertexForStop.get(toStop);
                if (!stopAccessibilityStrategy.stopIsAccessible(state, fromTransitStop).isAccessible()
                        || !stopAccessibilityStrategy.stopIsAccessible(state, toTransitStop).isAccessible()) {
                    return false;
                }
            }
        }

        return true;
    }

}
