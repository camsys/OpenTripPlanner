package org.opentripplanner.index.graphql.datafetchers;

import org.opentripplanner.index.graphql.generated.GraphQLDataFetchers;

import graphql.relay.Relay.ResolvedGlobalId;
import graphql.schema.DataFetcher;

public class GraphQLPatternImpl implements GraphQLDataFetchers.GraphQLPattern {

	@Override
	public DataFetcher<ResolvedGlobalId> id() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public DataFetcher<Object> route() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public DataFetcher<Integer> directionId() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public DataFetcher<String> name() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public DataFetcher<String> code() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public DataFetcher<String> headsign() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public DataFetcher<Iterable<Object>> stops() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public DataFetcher<String> semanticHash() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public DataFetcher<Iterable<Object>> alerts() {
		// TODO Auto-generated method stub
		return null;
	}

}
