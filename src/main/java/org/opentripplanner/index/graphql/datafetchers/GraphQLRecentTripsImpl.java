package org.opentripplanner.index.graphql.datafetchers;

import graphql.schema.DataFetchingEnvironment;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.onebusaway.gtfs.model.*;
import org.onebusaway.gtfs.model.calendar.ServiceDate;
import org.onebusaway.gtfs.services.calendar.CalendarService;
import org.opentripplanner.api.model.*;
import org.opentripplanner.api.resource.GraphPathToTripPlanConverter;
import org.opentripplanner.gtfs.GtfsLibrary;
import org.opentripplanner.index.graphql.GraphQLRequestContext;
import org.opentripplanner.index.model.*;
import org.opentripplanner.routing.algorithm.GenericDijkstra;
import org.opentripplanner.routing.algorithm.strategies.SkipEdgeStrategy;
import org.opentripplanner.routing.algorithm.strategies.SkipTraverseResultStrategy;
import org.opentripplanner.routing.core.*;
import org.opentripplanner.routing.edgetype.*;
import org.opentripplanner.routing.graph.Edge;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.graph.GraphIndex;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.routing.spt.GraphPath;
import org.opentripplanner.routing.spt.ShortestPathTree;
import org.opentripplanner.routing.trippattern.RealTimeState;
import org.opentripplanner.routing.trippattern.TripTimes;
import org.opentripplanner.routing.vertextype.TransitStop;
import org.opentripplanner.standalone.Router;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class GraphQLRecentTripsImpl {

    private static final Logger LOG = LoggerFactory.getLogger(GraphQLRecentTripsImpl.class);

    public List<Object> getRecentTrips(DataFetchingEnvironment environment) {

        Graph graph = getRouter(environment).graph;

        GraphQLQueryTypeInputs.GraphQLQueryTypeRecentTripsArgsInput input =
                new GraphQLQueryTypeInputs.GraphQLQueryTypeRecentTripsArgsInput(environment.getArguments());

        // GraphQL Arguments
        List<TripPattern> tripsPatterns = (List<TripPattern>) graph.index.patternsForFeedId.get(input.getGraphQLFeedId());


        List<Trip> trips = tripsPatterns.stream()
                .map(TripPattern::getTrips)
                .flatMap(List::stream)
                .collect(Collectors.toList());

        long currentTimeMillis = System.currentTimeMillis();

        List<Object> result = new ArrayList<>();

        for (Trip t : trips) {
            CalendarService calendarService = graph.getCalendarService();
            Set<ServiceDate> serviceDates = calendarService.getServiceDatesForServiceId(t.getServiceId());
            Set<Long> dates = serviceDates.stream().map(d -> d.getAsDate().getTime()).collect(Collectors.toSet());

            List<TripTimeShort> stopTimes = new ArrayList<>(TripTimeShort.fromTripTimes(
                    getGraphIndex(environment).getTripPatternForTripId(t.getId()).scheduledTimetable, t));
            for (Long date : dates) {
                long timeInterval = 8 * 60 * 60 * 1000;
                long time = stopTimes.get(0).scheduledDeparture * 1000L;

                if (Math.abs((date + time) - currentTimeMillis) < timeInterval) {
                    //trip ID, start date, direction, route ID, trip short name, and
                    // stop times for all trips that begin within +/- 8 hours of current time
                    Map<String, Object> tripInfo = new HashMap<>();

                    tripInfo.put("tripId", colonFormatAgency(t.getId()));

                    DateTimeFormatter fmt = DateTimeFormat.forPattern("yyyy-MM-dd");
                    tripInfo.put("startDate", new DateTime(date).toString(fmt));
                    tripInfo.put("direction", t.getDirectionId());
                    tripInfo.put("blockId", t.getBlockId());
                    tripInfo.put("routeShortName", t.getRouteShortName());
                    tripInfo.put("route", colonFormatAgency(t.getRoute().getId()));
                    tripInfo.put("tripShortName", t.getTripShortName());
                    tripInfo.put("tripHeadSign", t.getTripHeadsign());
                    tripInfo.put("stopTimes", stopTimes);
                    result.add(tripInfo);
                    break;
                }
            }
        }


        return result;
    }

    private String colonFormatAgency(AgencyAndId aid) {
        return aid == null ? null : aid.getAgencyId() + ':' + aid.getId();
    }

    private Router getRouter(DataFetchingEnvironment environment) {
        return environment.<GraphQLRequestContext>getContext().getRouter();
    }

    private GraphIndex getGraphIndex(DataFetchingEnvironment environment) {
        return environment.<GraphQLRequestContext>getContext().getIndex();
    }

}

