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

import com.google.protobuf.ExtensionRegistry;
import com.google.transit.realtime.GtfsRealtimeNYCT;
import org.junit.BeforeClass;
import org.opentripplanner.ConstantsForTests;
import org.opentripplanner.graph_builder.module.CrossFeedTransferGenerator;
import org.opentripplanner.routing.accessibility.MTAStopAccessibilityStrategy;
import org.opentripplanner.routing.algorithm.AStar;
import org.opentripplanner.routing.core.RoutingRequest;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.impl.AlertPatchServiceImpl;
import org.opentripplanner.routing.impl.DefaultStreetVertexIndexFactory;
import org.opentripplanner.routing.spt.GraphPath;
import org.opentripplanner.routing.spt.ShortestPathTree;
import org.opentripplanner.routing.transfers.MTATransferPermissionStrategy;
import org.opentripplanner.updater.alerts.AlertsUpdateHandler;

import java.io.File;
import java.util.HashMap;

public class MTAGraphTest {
    private static final String NYCT_SUBWAYS_GTFS = "src/test/resources/mta/nyct_subways_gtfs.zip";

    private static final String FEED_TRANSFERS = "src/test/resources/mta/feed_transfers.txt";

    protected static final String FEED_ID = "MTASBWY";

    protected static ExtensionRegistry _extensionRegistry;

    protected static Graph graph;

    protected static  AlertsUpdateHandler alertsUpdateHandler;

    private static AlertPatchServiceImpl alertPatchServiceImpl;

    @BeforeClass
    public static void setUpClass() throws Exception {
        _extensionRegistry = ExtensionRegistry.newInstance();
        _extensionRegistry.add(GtfsRealtimeNYCT.nyctFeedHeader);
        _extensionRegistry.add(GtfsRealtimeNYCT.nyctTripDescriptor);
        _extensionRegistry.add(GtfsRealtimeNYCT.nyctStopTimeUpdate);

        graph = ConstantsForTests.buildGraph(NYCT_SUBWAYS_GTFS);
        CrossFeedTransferGenerator transfers = new CrossFeedTransferGenerator(new File(FEED_TRANSFERS));
        transfers.buildGraph(graph, new HashMap<>());
        graph.index(new DefaultStreetVertexIndexFactory());
        graph.stopAccessibilityStrategy = new MTAStopAccessibilityStrategy(graph);
        graph.transferPermissionStrategy = new MTATransferPermissionStrategy(graph);

        alertsUpdateHandler = new AlertsUpdateHandler();
        alertPatchServiceImpl = new AlertPatchServiceImpl(graph);
        alertsUpdateHandler.setAlertPatchService(alertPatchServiceImpl);
        alertsUpdateHandler.setFeedId(FEED_ID);
    }

    protected void expireAlerts() {
        alertPatchServiceImpl.expireAll();
    }

    protected GraphPath search(String from, String to, String date, String time) {
        return search(from, to, date, time, getOptions());
    }

    protected GraphPath search(String from, String to, String date, String time, RoutingRequest options) {
        AStar aStar = new AStar();
        options.setDateTime(date, time, graph.getTimeZone());
        options.setRoutingContext(graph, FEED_ID+":" + from, FEED_ID + ":" + to);
        ShortestPathTree spt = aStar.getShortestPathTree(options);
        return spt.getPath(graph.getVertex(FEED_ID+":" + to), true);
    }

    protected RoutingRequest getOptions() {
        return new RoutingRequest();
    }

}
