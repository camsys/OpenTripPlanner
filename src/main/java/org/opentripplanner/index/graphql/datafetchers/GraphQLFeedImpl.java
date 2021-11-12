package org.opentripplanner.index.graphql.datafetchers;

import java.util.stream.Collectors;

import org.onebusaway.gtfs.model.Agency;
import org.onebusaway.gtfs.model.FeedInfo;
import org.opentripplanner.index.graphql.GraphQLRequestContext;
import org.opentripplanner.index.graphql.generated.GraphQLDataFetchers;
import org.opentripplanner.routing.graph.GraphIndex;

import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;

public class GraphQLFeedImpl implements GraphQLDataFetchers.GraphQLFeed {

	@Override
	public DataFetcher<String> feedId() {
	    return environment -> {
	    	FeedInfo e = environment.getSource();
	    	return getGraphIndex(environment).feedInfoForId.get(e.getId()).getId();
	    };
	}

	@Override
	public DataFetcher<Iterable<Object>> agencies() {
	    return environment -> {
	    	FeedInfo e = environment.getSource();
	    	return getGraphIndex(environment).agenciesForFeedId.get(e.getId())
	    			.values().stream().collect(Collectors.toList());
	    };
	}

	@Override
	public DataFetcher<String> feedVersion() {
	    return environment -> {
	    	FeedInfo e = environment.getSource();
	    	return getGraphIndex(environment).feedInfoForId.get(e.getId()).getVersion();
	    };
	}

	@Override
	public DataFetcher<Iterable<Object>> stops() {
	    return environment -> {
	    	FeedInfo e = environment.getSource();

	    	return getGraphIndex(environment).stopForId.entrySet().stream()
	    		.filter(o -> o.getKey().getAgencyId().equals(e.getId()))
	    		.map(s -> s.getValue())
	    		.collect(Collectors.toList());
	    };
	}
	
	@Override
	public DataFetcher<Iterable<Object>> routes() {
	    return environment -> {
	    	FeedInfo e = environment.getSource();

	    	return getGraphIndex(environment).routeForId.values().stream()
	    		.filter(o -> o.getId().getAgencyId().equals(e.getId()))
	    		.distinct()
	    		.collect(Collectors.toList());
	    };
	}
	
	private GraphIndex getGraphIndex(DataFetchingEnvironment environment) {
		return environment.<GraphQLRequestContext>getContext().getIndex();
	}

}
