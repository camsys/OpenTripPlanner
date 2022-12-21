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
import org.opentripplanner.api.common.LocationStringParser;
import org.opentripplanner.graph_builder.GraphBuilder;
import org.opentripplanner.graph_builder.module.NearbyStopFinder;
import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.routing.algorithm.RoutingWorker;
import org.opentripplanner.routing.algorithm.raptor.transit.AccessEgress;
import org.opentripplanner.routing.algorithm.raptor.transit.Transfer;
import org.opentripplanner.routing.algorithm.raptor.transit.TripSchedule;
import org.opentripplanner.routing.algorithm.raptor.transit.mappers.AccessEgressMapper;
import org.opentripplanner.routing.algorithm.raptor.transit.mappers.RaptorRequestMapper;
import org.opentripplanner.routing.algorithm.raptor.transit.request.RaptorRoutingRequestTransitData;
import org.opentripplanner.routing.algorithm.raptor.transit.request.RoutingRequestTransitDataProviderFilter;
import org.opentripplanner.routing.api.request.RoutingRequest;
import org.opentripplanner.routing.connectivity.MTAStopAccessibilityStrategy;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.graph.Edge;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.graph.SerializedGraphObject;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.routing.graphfinder.NearbyStop;
import org.opentripplanner.routing.vertextype.TransitEntranceVertex;
import org.opentripplanner.routing.vertextype.TransitStopVertex;
import org.opentripplanner.standalone.config.CommandLineParameters;
import org.opentripplanner.standalone.configure.OTPAppConstruction;
import org.opentripplanner.standalone.server.Router;
import org.opentripplanner.transit.raptor.RaptorService;
import org.opentripplanner.transit.raptor.api.request.RaptorRequest;
import org.opentripplanner.transit.raptor.api.response.RaptorResponse;
import org.opentripplanner.updater.alerts.AlertsUpdateHandler;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;


public class MTAGraphTest {
    private static final String NYCT_SUBWAYS_GTFS = "src/test/resources/mta/nyct_subways_gtfs.zip";

    private static final String NYCT_OSM = "src/test/resources/mta/nyc-region.osm.pbf";

    protected static final String FEED_ID = "MTASBWY";

    protected static ExtensionRegistry _extensionRegistry;

    protected static Graph graph;

    protected static Router router;

    protected static  AlertsUpdateHandler alertsUpdateHandler;

//    private static AlertPatchServiceImpl alertPatchServiceImpl;

    @BeforeClass
    public static void setUpClass() throws Exception {
        _extensionRegistry = ExtensionRegistry.newInstance();
        _extensionRegistry.add(GtfsRealtimeNYCT.nyctFeedHeader);
        _extensionRegistry.add(GtfsRealtimeNYCT.nyctTripDescriptor);
        _extensionRegistry.add(GtfsRealtimeNYCT.nyctStopTimeUpdate);
        System.setProperty("integration_test", "true");
        // this is super cool but I can't get it to work
        //graph = ConstantsForTests.buildGraph(NYCT_SUBWAYS_GTFS);

        CommandLineParameters params = new CommandLineParameters();
        params.build = true;
        params.loadStreet = true;
//        params.save = true;
        ArrayList<File> files = new ArrayList<>();
        files.add(new File("src/test/resources/mta"));
        params.baseDirectory = files;
        OTPAppConstruction app = new OTPAppConstruction(params);
        // here is where route-config is pulled in
        app.validateConfigAndDataSources();
        GraphBuilder builder = app.createGraphBuilder(graph);
        builder.run();
        graph = builder.getGraph();
        graph.stopAccessibilityStrategy = new MTAStopAccessibilityStrategy(graph);
        new SerializedGraphObject(graph, app.config().buildConfig(), app.config().routerConfig())
                .save(app.graphOutputDataSource());


//        CrossFeedTransferGenerator transfers = new CrossFeedTransferGenerator(new File(FEED_TRANSFERS));
//        transfers.buildGraph(graph, new HashMap<>());
//        graph.index();
//        graph.stopAccessibilityStrategy = new MTAStopAccessibilityStrategy(graph);
//        graph.transferPermissionStrategy = new MTATransferPermissionStrategy(graph);
//

        router = new Router(graph, app.config().routerConfig());
        router.startup();


//        alertsUpdateHandler = new AlertsUpdateHandler();
//        alertPatchServiceImpl = new AlertPatchServiceImpl(graph);
//        alertsUpdateHandler.setAlertPatchService(alertPatchServiceImpl);
//        alertsUpdateHandler.setFeedId(FEED_ID);
    }

//    protected void expireAlerts() {
//        alertPatchServiceImpl.expireAll();
//    }

    protected List<Itinerary> searchViaRoutingWorker(String from, String to, String date, String time) {
        return searchViaRoutingWorker(from, to, date, time, getOptions());
    }

    protected List<Itinerary> searchViaRoutingWorker(String from, String to, String date, String time, RoutingRequest options) {
        options.from = LocationStringParser.fromOldStyleString(FEED_ID+":" + from);
        options.to = LocationStringParser.fromOldStyleString(FEED_ID+":" + to);
        options.setDateTime(date, time, graph.getTimeZone());

        RoutingWorker worker = new RoutingWorker(router.raptorConfig, options);
        return worker.route(router).getTripPlan().itineraries;

    }

    protected RaptorResponse<TripSchedule> searchTransitEntrance(String from, String to, String date, String time, RoutingRequest request) {

        Collection<AccessEgress> accessList = new ArrayList<>();
        Collection<AccessEgress> egressList = new ArrayList<>();
        request.from = LocationStringParser.fromOldStyleString(FEED_ID+":" + from);
        request.to = LocationStringParser.fromOldStyleString(FEED_ID+":" + to);
        boolean accessible = request.wheelchairAccessible;

        request.setDateTime(date, time, graph.getTimeZone());

        TransitEntranceVertex entranceVertex = findTransitVertex(from, accessible);

        List<NearbyStop> nearbyAccessStops = findNearbyStops(entranceVertex, request, false);

        AccessEgressMapper accessEgressMapper = new AccessEgressMapper(graph.getTransitLayer().getStopIndex());

        for (NearbyStop nearbyAccessStop : nearbyAccessStops) {
            accessList.add(createAccessEgress(accessEgressMapper, nearbyAccessStop, false));
        }


        TransitEntranceVertex exitVertex = findTransitVertex(to, accessible);
        List<NearbyStop> nearbyEgressStops = findNearbyStops(exitVertex, request, true);
        for (NearbyStop nearbyEgressStop : nearbyEgressStops) {
            egressList.add(createAccessEgress(accessEgressMapper, nearbyEgressStop, true));
        }


        RoutingRequest transferRoutingRequest = Transfer.prepareTransferRoutingRequest(request);
        transferRoutingRequest.setRoutingContext(graph, (Vertex) null, null);
        RaptorRoutingRequestTransitData data = new RaptorRoutingRequestTransitData(
                graph.getTransitLayer(),
                request.getDateTime().toInstant(),
                request.additionalSearchDaysAfterToday,
                new RoutingRequestTransitDataProviderFilter(request, graph.index),
                transferRoutingRequest
        );

        if (accessList.isEmpty() || egressList.isEmpty()) {
            return null;
        }

        RaptorRequest<TripSchedule> raptorRequest = RaptorRequestMapper.mapRequest(
                request,
                data.getStartOfTime(),
                accessList,
                egressList
        );

        return new RaptorService<>(router.raptorConfig).route(raptorRequest, data);
    }

    private List<NearbyStop> findNearbyStops(TransitEntranceVertex vertex, RoutingRequest request, boolean isEgress) {
        if (vertex == null) return new ArrayList<>();
        List<NearbyStop> stops = new ArrayList<>();
        NearbyStopFinder finder = new NearbyStopFinder(graph, 300, false);
        ArrayList<String> directions = new ArrayList<>(List.of("N", "S"));

        for (NearbyStop nearbyStop : finder.findNearbyStops(vertex, request, isEgress)) {
            for (String direction : directions) {
                TransitStopVertex stopVertex = findStopVertex(vertex.getLabel().split("-")[0] + direction);
                stops.add(new NearbyStop(stopVertex,
                        nearbyStop.distance,
                        Collections.emptyList(),
                        new State(stopVertex, request)));
            }
        }
        return stops;
    }

    private TransitStopVertex findStopVertex(String id) {
        Vertex vertex = graph.getVertex(id);
        if (vertex == null)
            throw new IllegalStateException("vertex not found for id " + id);

        return (TransitStopVertex) vertex;
    }

    private AccessEgress createAccessEgress(AccessEgressMapper accessEgressMapper, NearbyStop nearbyStop, boolean isEgress) {
        AccessEgress accessEgress = accessEgressMapper.mapNearbyStop(nearbyStop, isEgress);
        assertNotNull(accessEgress);
        return accessEgress;
    }

    private TransitEntranceVertex findTransitVertex(String id, boolean accessible) {

        Vertex vertex = graph.getVertex(FEED_ID + ":" + id);
        if (vertex == null)
            throw new IllegalStateException("vertex not found for id " + id);

        TransitEntranceVertex transitEntranceVertex = (TransitEntranceVertex) vertex;
        if (accessible) {
            if (!transitEntranceVertex.isWheelchairEntrance()) {
                return null;
            }
        }
        return transitEntranceVertex;
    }

    protected RoutingRequest getOptions() {
        return new RoutingRequest();
    }

}
