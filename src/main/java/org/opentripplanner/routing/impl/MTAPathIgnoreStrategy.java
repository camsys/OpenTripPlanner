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
package org.opentripplanner.routing.impl;

import org.opentripplanner.routing.core.RoutingRequest;
import org.opentripplanner.routing.ignore.PathIgnoreStrategy;
import org.opentripplanner.routing.spt.GraphPath;

public class MTAPathIgnoreStrategy implements PathIgnoreStrategy {

    private double bestDuration = -1.0;

    @Override
    public boolean shouldIgnorePath(GraphPath path, RoutingRequest options) {
        // Keep Track of the shortest duration of all paths found so far.
        if (bestDuration == -1.0) {
            bestDuration = path.getDuration();
        } else if (bestDuration > path.getDuration()) {
            bestDuration = path.getDuration();
        }

        // Absurd Paths is a final check where various sanity checks can be applie to the path
        // e.g., don't include routes that involve 95% walking before getting on a very short bus ride.
        return pathIsAbsurd(path, bestDuration);
    }

    private boolean pathIsAbsurd(GraphPath path, double bestDuration) {

        // Check for Absurd Walks (e.g., walking 95% of the time and then taking something else for 5%.)
        // Question: What if that 1 thing is a ferry over water that you can't walk over?
        double walkRatio = path.getWalkTime() / path.getDuration();
        if (walkRatio > .95 && path.getRoutes().size() > 0)
            return true;

        // Check for Absurdly Long Trips when better options are better.
        // This says, if the trip is longer than 30 minutes and there is a solution that is twice as good that is already found, then don't include it.
        if (bestDuration * 2 < path.getDuration() && path.getDuration() > 1800) {
            return true;
        }

        return false;
    }
}
