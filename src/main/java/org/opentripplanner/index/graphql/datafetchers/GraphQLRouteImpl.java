package org.opentripplanner.index.graphql.datafetchers;

import java.util.Collection;
import java.util.stream.Collectors;

import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.gtfs.model.Route;
import org.onebusaway.gtfs.model.Trip;
import org.opentripplanner.gtfs.GtfsLibrary;
import org.opentripplanner.index.graphql.GraphQLRequestContext;
import org.opentripplanner.index.graphql.generated.GraphQLDataFetchers;
import org.opentripplanner.routing.edgetype.TripPattern;
import org.opentripplanner.routing.graph.GraphIndex;
import org.opentripplanner.standalone.Router;

import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;

public class GraphQLRouteImpl implements GraphQLDataFetchers.GraphQLRoute {

	@Override
	public DataFetcher<String> gtfsId() {
	    return environment -> {
	    	Route e = environment.getSource();
	    	return GtfsLibrary.convertIdToString(e.getId());
	    };
	}

	@Override
	public DataFetcher<Object> agency() {
	    return environment -> {
	    	Route e = environment.getSource();
	    	return e.getAgency();
	    };
	}

	@Override
	public DataFetcher<String> shortName() {
	    return environment -> {
	    	Route e = environment.getSource();
	    	return e.getShortName();
	    };
	}

	@Override
	public DataFetcher<String> longName() {
	    return environment -> {
	    	Route e = environment.getSource();
	    	return e.getLongName();
	    };
	}

	@Override
	public DataFetcher<Integer> type() {
	    return environment -> {
	    	Route e = environment.getSource();
	    	return e.getType();
	    };
	}

	@Override
	public DataFetcher<String> desc() {
	    return environment -> {
	    	Route e = environment.getSource();
	    	return e.getDesc();
	    };
	}

	@Override
	public DataFetcher<String> url() {
	    return environment -> {
	    	Route e = environment.getSource();
	    	return e.getUrl();
	    };
	}

	@Override
	public DataFetcher<String> color() {
	    return environment -> {
	    	Route e = environment.getSource();
	    	return e.getColor();
	    };
	}

	@Override
	public DataFetcher<String> textColor() {
	    return environment -> {
	    	Route e = environment.getSource();
	    	return e.getTextColor();
	    };
	}

	@Override
	public DataFetcher<Iterable<Object>> stops() {	    
	    return environment -> {
	    	Route e = environment.getSource();
	    	return getGraphIndex(environment).patternsForRoute.get(e)
	    			.stream()
	    			.flatMap(s -> s.getStops().stream())
					.distinct()
	    			.collect(Collectors.toList());
	    	};
	}

	@Override
	public DataFetcher<Iterable<Object>> trips() {
	    return environment -> {
	    	Route e = environment.getSource();	    	
	    	return getGraphIndex(environment).tripForId.values()
	    			.stream()
					.filter(s -> s.getRoute() != null ? s.getRoute().equals(e) : false)
					.distinct()
	    			.collect(Collectors.toList());
	    	};
	}


	@Override
	public DataFetcher<Iterable<Object>> alerts() {
		return environment -> {
	    	Route e = environment.getSource();

			return getRouter(environment).graph.getAlertPatches()
					.filter(s -> s.getRoute() != null ? s.getRoute().equals(e.getId()) : false)
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
