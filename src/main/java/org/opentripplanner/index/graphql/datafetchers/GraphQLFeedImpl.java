package org.opentripplanner.index.graphql.datafetchers;

import java.util.ArrayList;
import java.util.Collection;
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
	public DataFetcher<Iterable<Object>> routes() {
	    return environment -> {
	    	FeedInfo e = environment.getSource();

	    	Collection<Agency> agenciesToInclude = 
	    			getGraphIndex(environment).agenciesForFeedId.get(e.getId()).values();
	    	
	    	return getGraphIndex(environment).routeForId.values()
	    			.stream()
	    			.filter(it -> { return agenciesToInclude.contains(it.getAgency()); })
	    			.distinct()
	    			.collect(Collectors.toList());
	    };
	}
	
	@Override
	public DataFetcher<Iterable<Object>> trips() {
	    return environment -> {
	    	FeedInfo f = environment.getSource();

	    	ArrayList<String> agencyIdsToInclude = new ArrayList<String>();
	    	
	    	agencyIdsToInclude.addAll(getGraphIndex(environment).agenciesForFeedId.get(f.getId()).keySet());

	    	// feed and agency ID are used interchangably (a bug) by clients, 
	    	// so add the feedID to the list of agency IDs to look for
	    	agencyIdsToInclude.add(f.getId());
	    	
	    	return getGraphIndex(environment).tripForId.entrySet()
	    			.stream()
	    			.filter(it -> { return agencyIdsToInclude.contains(it.getKey().getAgencyId()); })
	    			.distinct()
	    			.collect(Collectors.toList());
	    };
	}

	private GraphIndex getGraphIndex(DataFetchingEnvironment environment) {
		return environment.<GraphQLRequestContext>getContext().getIndex();
	}

}
