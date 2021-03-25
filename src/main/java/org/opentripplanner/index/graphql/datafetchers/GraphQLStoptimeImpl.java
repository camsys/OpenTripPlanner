package org.opentripplanner.index.graphql.datafetchers;

import org.opentripplanner.index.graphql.GraphQLRequestContext;
import org.opentripplanner.index.graphql.generated.GraphQLDataFetchers;
import org.opentripplanner.index.model.TripTimeShort;
import org.opentripplanner.routing.graph.GraphIndex;

import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;

public class GraphQLStoptimeImpl implements GraphQLDataFetchers.GraphQLStoptime {

	@Override
	public DataFetcher<Object> stop() {
	    return environment -> {
	    	TripTimeShort t = environment.getSource();
	    	return getGraphIndex(environment).stopForId.get(t.stopId);
	    };	
	}

	@Override
	public DataFetcher<Integer> scheduledArrival() {
	    return environment -> {
	    	TripTimeShort t = environment.getSource();
	    	return t.scheduledArrival;
	    };	
	}

	@Override
	public DataFetcher<Integer> scheduledDeparture() {
	    return environment -> {
	    	TripTimeShort t = environment.getSource();
	    	return t.scheduledDeparture;
	    };	
	}
	
	private GraphIndex getGraphIndex(DataFetchingEnvironment environment) {
		return environment.<GraphQLRequestContext>getContext().getIndex();
	}

}
