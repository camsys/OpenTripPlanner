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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RTDPathIgnoreStrategy implements PathIgnoreStrategy {

    private static final Logger LOG = LoggerFactory.getLogger(RTDPathIgnoreStrategy.class);

    @Override
    public boolean shouldIgnorePath(GraphPath path, RoutingRequest options) {
        return graphPathStartsLaterThanLimit(path, options.tripShownRangeTime, options);
    }

    private static boolean graphPathStartsLaterThanLimit(GraphPath path, int range, RoutingRequest options) {
        boolean result = false;
        if (options.arriveBy) {
            long arrivetime = options.dateTime;
            if (arrivetime - path.getEndTime() > range) {
                result = true;
            }
        } else {
            long startTime = path.getStartTime();
            long departtime = options.dateTime;
            if (startTime - departtime > range) {
                result = true;
            }
        }
        return result;
    }

}
