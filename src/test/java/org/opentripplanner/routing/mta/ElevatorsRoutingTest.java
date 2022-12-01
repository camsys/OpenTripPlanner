///* This program is free software: you can redistribute it and/or
// modify it under the terms of the GNU Lesser General Public License
// as published by the Free Software Foundation, either version 3 of
// the License, or (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program.  If not, see <http://www.gnu.org/licenses/>. */
//package org.opentripplanner.routing.mta;
//
//import com.google.transit.realtime.GtfsRealtime;
//import com.google.transit.realtime.GtfsRealtimeConstants;
//import com.google.transit.realtime.GtfsRealtimeOneBusAway;
//import org.junit.Test;
//import org.opentripplanner.routing.core.RouteMatcher;
//import org.opentripplanner.routing.core.RoutingRequest;
//import org.opentripplanner.routing.spt.GraphPath;
//import org.opentripplanner.util.TestUtils;
//
//import java.util.Arrays;
//import java.util.List;
//
//import static org.junit.Assert.*;
//
//public class ElevatorsRoutingTest extends MTAGraphTest {
//
//    // 96th St (120S) and Chambers St (137S) are accessible
//    @Test
//    public void testCanRouteToAccessibleStops() {
//        RoutingRequest opt = new RoutingRequest();
//        opt.wheelchairAccessible = true;
//        GraphPath path = search("120-ent-acs", "137-ent-acs", "2018-03-15", "04:00pm", opt);
//        List<TestRide> rides = TestRide.createRides(path);
//        assertEquals(1, rides.size());
//        assertEquals("120S", rides.get(0).getFirstStopId());
//        assertEquals("137S", rides.get(0).getLastStopId());
//    }
//
//    // Trip: 96th 1/2/3 to Chambers 1/2/3
//    // EL145 is downtown elevator @ 96th, EL146 is uptown elevator
//    @Test
//    public void testStopBecomesNotAccessible() {
//        RoutingRequest opt = new RoutingRequest();
//        opt.wheelchairAccessible = true;
//        GtfsRealtime.FeedMessage message = elevatorFeedMessage("120", "EL145");
//        alertsUpdateHandler.update(message);
//        GraphPath path = search("120-ent-acs", "137-ent-acs", "2018-03-15", "04:00pm", opt);
//        // ok if we can't find anything
//        if (path != null) {
//            List<TestRide> rides = TestRide.createRides(path);
//            assertFalse(rides.get(0).getFirstStopId().equals("120S"));
//        }
//        expireAlerts();
//    }
//
//    @Test
//    public void testUptownNotAccessibleDowntownOK() {
//        RoutingRequest opt = new RoutingRequest();
//        opt.wheelchairAccessible = true;
//        GtfsRealtime.FeedMessage message = elevatorFeedMessage("120", "EL146");
//        alertsUpdateHandler.update(message);
//        GraphPath path = search("120-ent-acs", "137-ent-acs", "2018-03-15", "04:00pm", opt);
//        List<TestRide> rides = TestRide.createRides(path);
//        assertEquals(1, rides.size());
//        assertEquals("120S", rides.get(0).getFirstStopId());
//        assertEquals("137S", rides.get(0).getLastStopId());
//        expireAlerts();
//    }
//
//
//    // Franklin Av (A/C) A45S and Far Rockaway (A) H11S is accessible. You can transfer at Broadway Jct A51S, even though it is not accessible
//    @Test
//    public void testSamePlatformTransferNotAccessibleStop() {
//        RoutingRequest opt = new RoutingRequest();
//        opt.wheelchairAccessible = true;
//        opt.preferredStartRoutes = RouteMatcher.parse("MTASBWY__C");
//
//        GraphPath path = search("A45-ent-acs", "H11-ent-acs", "2018-03-15", "04:00pm", opt);
//        List<TestRide> rides = TestRide.createRides(path);
//        assertEquals(2, rides.size());
//        // test code actually prefers Euclid. But that's valid.
//        assertEquals("A45S", rides.get(0).getFirstStopId());
//        String stop0 =  rides.get(0).getLastStopId();
//        String stop1 = rides.get(1).getFirstStopId();
//        assertTrue(Arrays.asList("A55S", "A51S").contains(stop0));
//        assertEquals(stop0, stop1);
//        assertEquals("C", rides.get(0).getRoute().getId());
//        assertEquals("H11S", rides.get(1).getLastStopId());
//        assertEquals("A", rides.get(1).getRoute().getId());
//    }
//
//    // Accessible: 72St [123] 123S, 59St [1, D] 125S / A24S, Bay Pkwy [D] B21S
//    @Test
//    public void testBothStopsAccessibleTransfer() {
//        RoutingRequest opt = new RoutingRequest();
//        opt.wheelchairAccessible = true;
//        GraphPath path = search("123-ent-acs", "B21-ent-acs", "2018-03-15", "4:00pm", opt);
//        List<TestRide> rides = TestRide.createRides(path);
//        assertEquals(2, rides.size());
//        assertEquals("123S", rides.get(0).getFirstStopId());
//        assertTrue(rides.get(0).getLastStopId().equals("125S") || rides.get(0).getLastStopId().equals("235S")); // Columbus Cir or Barclays Ctr.
//        assertTrue(rides.get(0).getRoute().getId().equals("1") || rides.get(0).getRoute().getId().equals("2")); // Only 2 to Barclays obviously
//        assertTrue(rides.get(1).getFirstStopId().equals("A24S") || rides.get(1).getFirstStopId().equals("R31S")); // Columbus Cir or Barclays Ctr.
//        assertEquals("B21S", rides.get(1).getLastStopId());
//        assertEquals("D", rides.get(1).getRoute().getId());
//    }
//
//    // Check if 59St (1) becomes not accessible, the transfer is not accessible
//    @Test
//    public void testStopInTransferLosesAccessibility() {
//        RoutingRequest opt = new RoutingRequest();
//        opt.wheelchairAccessible = true;
//        GtfsRealtime.FeedMessage message = elevatorFeedMessage("125S", "EL277", "125S", "EL276X");
//        alertsUpdateHandler.update(message);
//
//        GraphPath path = search("123-ent-acs", "B21-ent-acs", "2018-03-15", "4:00pm", opt);
//        if (path != null) {
//            List<TestRide> rides = TestRide.createRides(path);
//
//            // can't get from the street to the platform via 125S
//            // the TP may get clever and route you to 72nd to get to 125S via
//            // a train
//            assertFalse(rides.get(0).getFirstStopId().equals("125S"));
//        }
//
//        expireAlerts();
//    }
//
//    // Check if unrelated elevators are out, the transfer is unaffected
//    @Test
//    public void testUnrelatedElevatorsForTransfer() {
//        GtfsRealtime.FeedMessage message = elevatorFeedMessage("125N", "EL278", "125N", "EL279", "125N", "EL276X");
//        alertsUpdateHandler.update(message);
//        GraphPath path = search("123-ent-acs", "B21-ent-acs", "2018-03-15", "4:00pm");
//        List<TestRide> rides = TestRide.createRides(path);
//        assertEquals(2, rides.size());
//        assertEquals("123S", rides.get(0).getFirstStopId());
//        assertEquals("125S", rides.get(0).getLastStopId());
//        assertEquals("1", rides.get(0).getRoute().getId());
//        assertEquals("A24S", rides.get(1).getFirstStopId());
//        assertEquals("B21S", rides.get(1).getLastStopId());
//        assertEquals("D", rides.get(1).getRoute().getId());
//        expireAlerts();
//    }
//
//    // Params are (stop, elevator) pairs
//    private GtfsRealtime.FeedMessage elevatorFeedMessage(String... params) {
//        long start = TestUtils.dateInSeconds("America/New_York", 2018, 2, 15, 15, 0, 0);
//        long end = TestUtils.dateInSeconds("America/New_York", 2018, 2, 15, 17, 0, 0);
//        return elevatorFeedMessage(start, end, params);
//    }
//
//    private GtfsRealtime.FeedMessage elevatorFeedMessage(long start, long end, String... params) {
//        GtfsRealtime.FeedHeader.Builder header = GtfsRealtime.FeedHeader.newBuilder();
//        header.setIncrementality(GtfsRealtime.FeedHeader.Incrementality.FULL_DATASET);
//        header.setTimestamp(start);
//        header.setGtfsRealtimeVersion(GtfsRealtimeConstants.VERSION);
//        GtfsRealtime.FeedMessage.Builder builder = GtfsRealtime.FeedMessage.newBuilder()
//                .setHeader(header);
//        for (int i = 0; i < params.length; i += 2) {
//            String stopId = params[i];
//            String elevatorId = params[i + 1];
//            builder.addEntity(elevatorAlertEntity(stopId, elevatorId, start, end));
//        }
//        return builder.build();
//    }
//
//    private GtfsRealtime.FeedEntity elevatorAlertEntity(String stopId, String elevatorId, long start, long end) {
//        GtfsRealtime.Alert.Builder alert = GtfsRealtime.Alert.newBuilder();
//        GtfsRealtimeOneBusAway.OneBusAwayEntitySelector elevatorExtension = GtfsRealtimeOneBusAway.OneBusAwayEntitySelector.newBuilder()
//                .setElevatorId(elevatorId).build();
//        GtfsRealtime.EntitySelector.Builder informedEntity = GtfsRealtime.EntitySelector.newBuilder()
//                .setStopId(stopId)
//                .setExtension(GtfsRealtimeOneBusAway.obaEntitySelector, elevatorExtension);
//        alert.addInformedEntity(informedEntity);
//        String desc = "Some description.";
//        alert.setHeaderText(translatedString(desc));
//        alert.setDescriptionText(translatedString(desc));
//        alert.addActivePeriod(GtfsRealtime.TimeRange.newBuilder()
//                .setStart(start)
//                .setEnd(end));
//        GtfsRealtime.FeedEntity.Builder builder = GtfsRealtime.FeedEntity.newBuilder()
//                .setAlert(alert)
//                .setId(stopId + "#" + elevatorId);
//        return builder.build();
//    }
//
//    private GtfsRealtime.TranslatedString.Builder translatedString(String str) {
//        return GtfsRealtime.TranslatedString.newBuilder()
//                .addTranslation(GtfsRealtime.TranslatedString.Translation.newBuilder()
//                        .setText(str)
//                        .setLanguage("en"));
//    }
//
//
//    @Override
//    protected RoutingRequest getOptions() {
//        RoutingRequest options = new RoutingRequest();
//        options.wheelchairAccessible = true;
//        return options;
//    }
//}
