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
package org.opentripplanner.routing.mta;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.google.transit.realtime.GtfsRealtime;
import org.junit.Before;
import org.junit.Test;
import org.opentripplanner.routing.spt.GraphPath;
import org.opentripplanner.routing.trippattern.RealTimeState;
import org.opentripplanner.routing.trippattern.TripTimes;
import org.opentripplanner.updater.stoptime.TimetableSnapshotSource;
import org.opentripplanner.util.TestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

public class AddedTripsRoutingTest extends MTAGraphTest {
    private static final String FEED_ID = "MTASBWY";

    private static final String NYCT_TU = "mta/tu-example-Mar-14-17:57:11-EDT-2018.pb";

    private static TimetableSnapshotSource updater;

    private static final Logger LOG = LoggerFactory.getLogger(AddedTripsRoutingTest.class);

    @Before
    public void before() throws Exception {

        updater = new TimetableSnapshotSource(graph);
        updater.purgeExpiredData = false;
        graph.timetableSnapshotSource = updater;

        InputStream pb = AddedTripsRoutingTest.class.getResourceAsStream("/" + NYCT_TU);
        GtfsRealtime.FeedMessage message = GtfsRealtime.FeedMessage.parseFrom(pb, _extensionRegistry);
        List<GtfsRealtime.TripUpdate> updates = message.getEntityList().stream()
                .filter(GtfsRealtime.FeedEntity::hasTripUpdate)
                .map(GtfsRealtime.FeedEntity::getTripUpdate)
                .collect(Collectors.toList());
        updater.applyTripUpdates(graph,  true, updates, FEED_ID, message.getHeader().getTimestamp());
    }

    @Test
    public void testFindsCancelledAndAddedZ() {
        GraphPath path = search("J23S", "G05S", "2018-03-14", "5:58pm");

        String hash = path.getRoutePatternHash();
        LOG.info("found path: {} from {} to {}", hash, new Date(path.getStartTime() * 1000), new Date(path.getEndTime() * 1000));

        // Make sure we found a Z trip that arrives within 60min
        List<Ride> rides = Ride.createRides(path);
        assertEquals(1, rides.size());
        assertEquals("104450_Z..S", rides.get(0).getTrip().getId());

        // month is 0-indexed
        long endLimit = TestUtils.dateInSeconds("America/New_York", 2018, 2, 14, 19, 0, 0);
        assertTrue(path.getEndTime() < endLimit);

        TripTimes tt = path.states.getLast().getTripTimes();
        assertEquals(tt.getRealTimeState(), RealTimeState.ADDED);
    }

    // B20171105WKD_105450_J..N22R is cancelled on 3/14, but not 3/15
    @Test
    public void testCancelledTripFutureDay() {
        GraphPath path = search("J23S", "G05S", "2018-03-15", "5:58pm");

        String hash = path.getRoutePatternHash();
        LOG.info("found path: {} from {} to {}", hash, new Date(path.getStartTime() * 1000), new Date(path.getEndTime() * 1000));

        List<Ride> rides = Ride.createRides(path);
        assertEquals(1, rides.size());
        assertEquals("B20171105WKD_105450_J..N22R", rides.get(0).getTrip().getId());
    }

}
