package org.opentripplanner.index.graphql.datafetchers;

import java.util.stream.Collectors;

import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.gtfs.model.FeedInfo;
import org.onebusaway.gtfs.model.Route;
import org.onebusaway.gtfs.model.Stop;
import org.onebusaway.gtfs.model.Trip;
import org.opentripplanner.gtfs.GtfsLibrary;
import org.opentripplanner.index.graphql.GraphQLRequestContext;
import org.opentripplanner.index.graphql.generated.GraphQLDataFetchers;
import org.opentripplanner.index.graphql.generated.GraphQLTypes.GraphQLBikesAllowed;
import org.opentripplanner.index.graphql.generated.GraphQLTypes.GraphQLWheelchairBoarding;
import org.opentripplanner.index.model.TripTimeShort;
import org.opentripplanner.routing.graph.GraphIndex;
import org.opentripplanner.standalone.Router;

import graphql.relay.Relay.ResolvedGlobalId;
import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;

public class GraphQLTripImpl implements GraphQLDataFetchers.GraphQLTrip {

	@Override
	public DataFetcher<String> gtfsId() {
	    return environment -> {
	    	Trip t = environment.getSource();
	    	return GtfsLibrary.convertIdToString(t.getId());
	    };
	}

	@Override
	public DataFetcher<String> serviceId() {
	    return environment -> {
	    	Trip t = environment.getSource();
	    	return GtfsLibrary.convertIdToString(t.getServiceId());
	    };
	}

	@Override
	public DataFetcher<String> tripShortName() {
	    return environment -> {
	    	Trip t = environment.getSource();
	    	return t.getTripShortName();
	    };
	}

	@Override
	public DataFetcher<String> tripHeadsign() {
	    return environment -> {
	    	Trip t = environment.getSource();
	    	return t.getTripHeadsign();
	    };
	}

	@Override
	public DataFetcher<String> routeShortName() {
	    return environment -> {
	    	Trip t = environment.getSource();
	    	return t.getRouteShortName();
	    };
	}

	@Override
	public DataFetcher<String> directionId() {
	    return environment -> {
	    	Trip t = environment.getSource();
	    	return t.getDirectionId();
	    };
	}

	@Override
	public DataFetcher<String> blockId() {
	    return environment -> {
	    	Trip t = environment.getSource();
	    	return t.getBlockId();
	    };
	}

	@Override
	public DataFetcher<String> shapeId() {
	    return environment -> {
	    	Trip t = environment.getSource();
	    	return GtfsLibrary.convertIdToString(t.getShapeId());
	    };
	}

	@Override
	public DataFetcher<Iterable<Object>> stoptimes() {
	    return environment -> {
	    	Trip t = environment.getSource();
	    	return TripTimeShort.fromTripTimes(
	    			getGraphIndex(environment).getTripPatternForTripId(t.getId()).scheduledTimetable, t)
					.stream()
					.distinct()
	    			.collect(Collectors.toList());
	    };
	}
	

	@Override
	public DataFetcher<Object> route() {
	    return environment -> {
	    	Trip t = environment.getSource();
	    	return t.getRoute();
	    };
	}

	@Override
	public DataFetcher<String> wheelchairAccessible() {
	    return environment -> {
	    	Trip t = environment.getSource();
	    	return GraphQLWheelchairBoarding.values()[t.getWheelchairAccessible()].name();
	    };
	}

	@Override
	public DataFetcher<String> bikesAllowed() {
	    return environment -> {
	    	Trip t = environment.getSource();
	    	return GraphQLBikesAllowed.values()[t.getBikesAllowed()].name();
	    };
	}
	
	@Override
	public DataFetcher<Iterable<Object>> alerts() {
		return environment -> {
	    	Trip t = environment.getSource();

			return getRouter(environment).graph.getAlertPatches()
					.filter(s -> s.getTrip() != null ? s.getTrip().equals(t.getId()) : false)
					.collect(Collectors.toList());
		};	
	}
	
	private Router getRouter(DataFetchingEnvironment environment) {
		return environment.<GraphQLRequestContext>getContext().getRouter();
	}
	
	private GraphIndex getGraphIndex(DataFetchingEnvironment environment) {
		return environment.<GraphQLRequestContext>getContext().getIndex();
	}

}
