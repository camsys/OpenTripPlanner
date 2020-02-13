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

import com.google.transit.realtime.GtfsRealtime;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;
import org.opentripplanner.routing.alertpatch.AlertPatch;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.services.AlertPatchService;
import org.opentripplanner.util.TestUtils;

import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class VehiclePositionsUpdateHandlerTest {

    @Spy
    private MockAlertPatchService alertPatchService;

    @Test
    public void testVehiclePosition() {
        VehiclePositionsUpdateHandler handler = new VehiclePositionsUpdateHandler();
        handler.setAlertPatchService(alertPatchService);
        handler.setFeedId("feed");

        GtfsRealtime.FeedMessage message = GtfsRealtime.FeedMessage.newBuilder()
                .setHeader(GtfsRealtime.FeedHeader.newBuilder().setTimestamp(1).setGtfsRealtimeVersion("1.0"))
                .addEntity(GtfsRealtime.FeedEntity.newBuilder().setVehicle(
                        GtfsRealtime.VehiclePosition.newBuilder()
                                .setVehicle(GtfsRealtime.VehicleDescriptor.newBuilder().setId("vehicle1"))
                                .setTrip(GtfsRealtime.TripDescriptor.newBuilder().setTripId("trip1"))
                                .setTimestamp(TestUtils.dateInSeconds
                                        ("America/New_York", 2018, 6, 15, 11,
                                                0, 0))
                ).setId("1")).build();
        handler.update(message);

        Set<AlertPatch> patches = alertPatchService.patches;
        assertEquals(1, patches.size());
        AlertPatch patch = patches.iterator().next();
        assertEquals("vehicle1", patch.getVehicleInfo().getVehicleId());
        assertEquals("trip1", patch.getTrip().getId());
        long todayMidnight = TestUtils.dateInSeconds("America/New_York", 2018,
                6, 15, 0, 0, 0);
        long tomorrowMidnight = TestUtils.dateInSeconds("America/New_York", 2018,
                6, 16, 0, 0, 0);
        assertFalse(patch.displayDuring(mockState(todayMidnight - 1)));
        assertTrue(patch.displayDuring(mockState(todayMidnight + 1)));
        assertFalse(patch.displayDuring(mockState(tomorrowMidnight + 1)));
        assertTrue(patch.displayDuring(mockState(tomorrowMidnight - 1)));
    }

    State mockState(long time) {
        State state = mock(State.class);
        when(state.getStartTimeSeconds()).thenReturn(time);
        when(state.getTimeSeconds()).thenReturn(time);
        return state;
    }

    static abstract class MockAlertPatchService implements AlertPatchService {

        private Set<AlertPatch> patches = new HashSet<>();

        @Override
        public void apply(AlertPatch patch) {
            patches.add(patch);
        }
    }
}
