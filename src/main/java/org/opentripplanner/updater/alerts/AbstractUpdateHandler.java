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

package org.opentripplanner.updater.alerts;

import com.google.transit.realtime.GtfsRealtime.FeedMessage;
import org.opentripplanner.routing.services.AlertPatchService;
import org.opentripplanner.updater.GtfsRealtimeFuzzyTripMatcher;

import java.util.HashSet;
import java.util.Set;

/**
 * @author novalis
 *
 */
public abstract class AbstractUpdateHandler {

    String feedId;

    Set<String> patchIds = new HashSet<String>();

    AlertPatchService alertPatchService;

    /** How long before the posted start of an event it should be displayed to users */
    long earlyStart;

    /** Set only if we should attempt to match the trip_id from other data in TripDescriptor */
    GtfsRealtimeFuzzyTripMatcher fuzzyTripMatcher;

    abstract public void update(FeedMessage message);

    public void setFeedId(String feedId) {
        if(feedId != null)
            this.feedId = feedId.intern();
    }

    public void setAlertPatchService(AlertPatchService alertPatchService) {
        this.alertPatchService = alertPatchService;
    }

    public long getEarlyStart() {
        return earlyStart;
    }

    public void setEarlyStart(long earlyStart) {
        this.earlyStart = earlyStart;
    }

    public void setFuzzyTripMatcher(GtfsRealtimeFuzzyTripMatcher fuzzyTripMatcher) {
        this.fuzzyTripMatcher = fuzzyTripMatcher;
    }
}
