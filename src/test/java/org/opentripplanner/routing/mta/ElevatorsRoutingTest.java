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

import org.junit.Test;
import org.opentripplanner.model.StopLocation;
import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.model.plan.Leg;
import org.opentripplanner.routing.algorithm.raptor.transit.TripSchedule;
import org.opentripplanner.routing.api.request.RoutingRequest;
import org.opentripplanner.transit.raptor.api.path.Path;
import org.opentripplanner.transit.raptor.api.response.RaptorResponse;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

public class ElevatorsRoutingTest extends MTAGraphTest {


    // 96th St (120S) and Chambers St (137S) are accessible
    @Test
    public void testCanRouteToAccessibleStopsViaCoordinates() {

        if (graph.hasStreets) {
            // this test requires OSM, copy to src/test/resources/mta
            RoutingRequest opt = new RoutingRequest();
            List<Itinerary> itineraries = searchViaRoutingWorker("40.793919,-73.972323", "40.715478,-74.009266", "2018-03-15", "04:00pm", opt);
            assertEquals(2, itineraries.size());
            Itinerary i = itineraries.get(0);
            assertNotNull(i.legs.get(0));
            Leg from = i.legs.get(0);
            assertNotNull(from.from);
            assertNotNull(from.from.stopId);
            assertEquals("120S", from.from.stopId.getId());
        }

    }


    /**
       96th St (120S) and Chambers St (137S) are accessible
       NOTE:  This is only a partial test, as RoutingWorker requires OSM.
       INSTEAD, this test finds stops based on entrances marked accessible, then
       directly runs raptor on those stops.
     */
    @Test
    public void testCanRouteToAccessibleStops() {
        RoutingRequest opt = new RoutingRequest();
        opt.wheelchairAccessible = true;

        RaptorResponse<TripSchedule> response = searchTransitEntrance("120-ent-acs", "137-ent-acs", "2018-03-15", "04:00pm", opt);
        assertNotNull(response);
        assertTrue(!response.paths().isEmpty());

        ArrayList<Path<TripSchedule>> paths = new ArrayList<>(response.paths());
        Path<TripSchedule> accessleg = paths.get(0);
        StopLocation access = graph.getTransitLayer().getStopByIndex(accessleg.accessLeg().toStop());

        Path<TripSchedule> egressLeg = paths.get(paths.size()-1);

        StopLocation egress = graph.getTransitLayer().getStopByIndex(egressLeg.egressLeg().fromStop());
        assertEquals("MTASBWY:120S", access.getId().toString());
        assertEquals("MTASBWY:137S", egress.getId().toString());

    }

    @Test
    public void testCantRouteToInAccessibleStops() {
        RoutingRequest opt = new RoutingRequest();
        // first search non-accessible
        RaptorResponse<TripSchedule> response = searchTransitEntrance("120-entrance-5", "137-ent-acs", "2018-03-15", "04:00pm", opt);
        assertNotNull(response);
        assertTrue(!response.paths().isEmpty());

        ArrayList<Path<TripSchedule>> paths = new ArrayList<>(response.paths());
        Path<TripSchedule> accessleg = paths.get(0);
        StopLocation access = graph.getTransitLayer().getStopByIndex(accessleg.accessLeg().toStop());

        Path<TripSchedule> egressLeg = paths.get(paths.size()-1);

        StopLocation egress = graph.getTransitLayer().getStopByIndex(egressLeg.egressLeg().fromStop());
        assertEquals("MTASBWY:120S", access.getId().toString());
        assertEquals("MTASBWY:137S", egress.getId().toString());

        opt = new RoutingRequest();
        opt.wheelchairAccessible = true;
        // now search accessible
        response = searchTransitEntrance("120-entrance-5", "137-ent-acs", "2018-03-15", "04:00pm", opt);
        // test gave up
        assertNull(response);


    }


    @Override
    protected RoutingRequest getOptions() {
        RoutingRequest options = new RoutingRequest();
        options.wheelchairAccessible = true;
        return options;
    }
}
