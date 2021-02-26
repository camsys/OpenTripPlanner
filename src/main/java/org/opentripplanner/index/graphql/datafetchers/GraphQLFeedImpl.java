package org.opentripplanner.index.graphql.datafetchers;

import java.util.stream.Collectors;

import org.opentripplanner.index.graphql.generated.GraphQLDataFetchers;

import graphql.schema.DataFetcher;

public class GraphQLFeedImpl implements GraphQLDataFetchers.GraphQLFeed {

	@Override
	public DataFetcher<String> feedId() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public DataFetcher<Iterable<Object>> agencies() {
		// TODO Auto-generated method stub
		return null;
	}


}
