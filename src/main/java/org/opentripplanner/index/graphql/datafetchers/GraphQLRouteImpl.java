package org.opentripplanner.index.graphql.datafetchers;

import org.opentripplanner.index.graphql.generated.GraphQLDataFetchers;

import graphql.relay.Relay.ResolvedGlobalId;
import graphql.schema.DataFetcher;

public class GraphQLRouteImpl implements GraphQLDataFetchers.GraphQLRoute {

	@Override
	public DataFetcher<ResolvedGlobalId> id() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public DataFetcher<String> gtfsId() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public DataFetcher<Object> agency() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public DataFetcher<String> shortName() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public DataFetcher<String> longName() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public DataFetcher<Integer> type() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public DataFetcher<String> desc() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public DataFetcher<String> url() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public DataFetcher<String> color() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public DataFetcher<String> textColor() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public DataFetcher<Iterable<Object>> patterns() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public DataFetcher<Iterable<Object>> stops() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public DataFetcher<Iterable<Object>> alerts() {
		// TODO Auto-generated method stub
		return null;
	}

}
