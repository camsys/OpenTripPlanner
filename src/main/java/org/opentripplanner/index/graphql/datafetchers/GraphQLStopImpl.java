package org.opentripplanner.index.graphql.datafetchers;

import graphql.relay.Relay;
import graphql.relay.Relay.ResolvedGlobalId;
import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;

import java.util.List;
import java.util.stream.Collectors;
import org.onebusaway.gtfs.model.Stop;

import org.onebusaway.gtfs.model.AgencyAndId;
import org.opentripplanner.index.graphql.GraphQLRequestContext;
import org.opentripplanner.index.graphql.generated.GraphQLDataFetchers;
import org.opentripplanner.routing.graph.GraphIndex;
import org.opentripplanner.standalone.Router;

public class GraphQLStopImpl implements GraphQLDataFetchers.GraphQLStop {

	@Override
	public DataFetcher<ResolvedGlobalId> id() {
	    return environment -> {
	    	Stop e = environment.getSource();
	    	return new Relay.ResolvedGlobalId("Stop", AgencyAndId.convertToString(e.getId()));
	    };
	}

	@Override
	public DataFetcher<String> gtfsId() {
	    return environment -> {
	    	Stop e = environment.getSource();
	    	return AgencyAndId.convertToString(e.getId());
	    };
	}

	@Override
	public DataFetcher<String> name() {
	    return environment -> {
	    	Stop e = environment.getSource();
	    	return e.getName();
	    };
	}

	@Override
	public DataFetcher<Double> lat() {
	    return environment -> {
	    	Stop e = environment.getSource();
	    	return e.getLat();
	    };	
	 }

	@Override
	public DataFetcher<Double> lon() {
	    return environment -> {
	    	Stop e = environment.getSource();
	    	return e.getLon();
	    };
	}

	@Override
	public DataFetcher<String> code() {
	    return environment -> {
	    	Stop e = environment.getSource();
	    	return e.getCode();
	    };
	}

	@Override
	public DataFetcher<String> desc() {
	    return environment -> {
	    	Stop e = environment.getSource();
	    	return e.getDesc();
	    };
	}

	@Override
	public DataFetcher<String> zoneId() {
	    return environment -> {
	    	Stop e = environment.getSource();
	    	return e.getZoneId();
	    };
	}

	@Override
	public DataFetcher<String> url() {
	    return environment -> {
	    	Stop e = environment.getSource();
	    	return e.getUrl();
	    };
	}

	@Override
	public DataFetcher<Object> locationType() {
	    return environment -> {
	    	Stop e = environment.getSource();
	    	return e.getLocationType();
	    };
	}

	@Override
	public DataFetcher<Object> parentStation() {
	    return environment -> {
	    	Stop e = environment.getSource();
	    	return e.getParentStation();
	    };
	}

	@Override
	public DataFetcher<Object> wheelchairBoarding() {
	    return environment -> {
	    	Stop e = environment.getSource();
	    	return e.getWheelchairBoarding();
	    };
	}

	@Override
	public DataFetcher<String> direction() {
	    return environment -> {
	    	Stop e = environment.getSource();
	    	return e.getDirection();
	    };
	}

	@Override
	public DataFetcher<String> timezone() {
	    return environment -> {
	    	Stop e = environment.getSource();
	    	return e.getTimezone();
	    };
	}

	@Override
	public DataFetcher<Integer> vehicleType() {
	    return environment -> {
	    	Stop e = environment.getSource();
	    	return e.getVehicleType();
	    };
	}

	@Override
	public DataFetcher<String> platformCode() {
	    return environment -> {
	    	Stop e = environment.getSource();
	    	return e.getPlatformCode();
	    };
	}

	@Override
	public DataFetcher<Iterable<Object>> stops() {
	    return environment -> {
	    	Stop e = environment.getSource();
	    	AgencyAndId stationId = e.getId();
	    	
	    	return getGraphIndex(environment).stopsForParentStation
	    			.get(stationId).stream().collect(Collectors.toList());
	    };
	}

	@Override
	public DataFetcher<Iterable<Object>> routes() {
	    return environment -> {
	    	Stop e = environment.getSource();
	    	
	    	return getGraphIndex(environment).routesForStop(e).stream().collect(Collectors.toList());
	    };
	}

	@Override
	public DataFetcher<Iterable<Object>> alerts() {
		return environment -> List.of("__NOT IMPLEMENTED__");
	}

	private Router getRouter(DataFetchingEnvironment environment) {
		return environment.<GraphQLRequestContext>getContext().getRouter();
	}

	private GraphIndex getGraphIndex(DataFetchingEnvironment environment) {
		return environment.<GraphQLRequestContext>getContext().getIndex();
	}
}
