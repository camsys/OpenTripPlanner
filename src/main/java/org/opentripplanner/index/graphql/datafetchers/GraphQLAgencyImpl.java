package org.opentripplanner.index.graphql.datafetchers;

import graphql.relay.Relay;
import graphql.relay.Relay.ResolvedGlobalId;
import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.opentripplanner.index.graphql.generated.GraphQLDataFetchers;

public class GraphQLAgencyImpl implements GraphQLDataFetchers.GraphQLAgency {

	@Override
	public DataFetcher<ResolvedGlobalId> id() {
		return environment -> null;
	}

	@Override
	public DataFetcher<String> gtfsId() {
		return environment -> null;
	}

	@Override
	public DataFetcher<String> name() {
		return environment -> null;
	}

	@Override
	public DataFetcher<String> url() {
		return environment -> null;
	}

	@Override
	public DataFetcher<String> timezone() {
		return environment -> null;
	}

	@Override
	public DataFetcher<String> lang() {
		return environment -> null;
	}

	@Override
	public DataFetcher<String> phone() {
		return environment -> null;
	}

	@Override
	public DataFetcher<String> fareUrl() {
		return environment -> null;
	}

	@Override
	public DataFetcher<Iterable<Object>> routes() {
		return environment -> List.of();
	}

	@Override
	public DataFetcher<Iterable<Object>> alerts() {
		return environment -> List.of();
	}
}
