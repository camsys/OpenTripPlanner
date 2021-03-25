package org.opentripplanner.index.graphql.datafetchers;

import java.util.stream.Collectors;

import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.gtfs.model.FeedInfo;
import org.onebusaway.gtfs.model.Route;
import org.onebusaway.gtfs.model.Trip;
import org.opentripplanner.index.graphql.GraphQLRequestContext;
import org.opentripplanner.index.graphql.generated.GraphQLDataFetchers;
import org.opentripplanner.index.model.TripTimeShort;
import org.opentripplanner.routing.graph.GraphIndex;

import graphql.relay.Relay.ResolvedGlobalId;
import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;

public class GraphQLTripImpl implements GraphQLDataFetchers.GraphQLTrip {

	@Override
	public DataFetcher<String> gtfsId() {
	    return environment -> {
	    	Trip t = environment.getSource();
	    	return AgencyAndId.convertToString(t.getId());
	    };
	}

	@Override
	public DataFetcher<String> serviceId() {
	    return environment -> {
	    	Trip t = environment.getSource();
	    	return AgencyAndId.convertToString(t.getServiceId());
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
	    	return AgencyAndId.convertToString(t.getShapeId());
	    };
	}

	@Override
	public DataFetcher<Iterable<Object>> stoptimes() {
	    return environment -> {
	    	Trip t = environment.getSource();
	    	return TripTimeShort.fromTripTimes(
	    			getGraphIndex(environment).patternForTrip.get(t).scheduledTimetable, t)
	    			.stream().collect(Collectors.toList());
	    };
	}
	
	private GraphIndex getGraphIndex(DataFetchingEnvironment environment) {
		return environment.<GraphQLRequestContext>getContext().getIndex();
	}

}
