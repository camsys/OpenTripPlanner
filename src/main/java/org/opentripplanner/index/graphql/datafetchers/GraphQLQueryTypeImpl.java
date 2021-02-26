package org.opentripplanner.index.graphql.datafetchers;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.onebusaway.gtfs.model.AgencyAndId;
import org.opentripplanner.index.graphql.GraphQLRequestContext;
import org.opentripplanner.index.graphql.generated.GraphQLDataFetchers;
import org.opentripplanner.index.graphql.generated.GraphQLTypes;
import org.opentripplanner.routing.graph.GraphIndex;
import org.opentripplanner.standalone.Router;

import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;

public class GraphQLQueryTypeImpl implements GraphQLDataFetchers.GraphQLQueryType {

	@Override
	public DataFetcher<Object> node() {
		return environment -> null;
	}

	@Override
	public DataFetcher<Object> stop() {
	    return environment -> getGraphIndex(environment)
	            .stopForId.get(AgencyAndId.convertFromString(
	                new GraphQLTypes.GraphQLQueryTypeStopArgs(environment.getArguments()).getGraphQLId()));
	}

	@Override
	public DataFetcher<Iterable<Object>> alerts() {
		return environment->List.of();
	}

	@Override
	public DataFetcher<Iterable<Object>> feeds() {
		return environment->List.of();
	}

	@Override
	public DataFetcher<Iterable<Object>> agencies() {
		return environment->List.of();
	}

	@Override
	public DataFetcher<Object> agency() {
		return environment -> null;
	}

	@Override
	public DataFetcher<Iterable<Object>> stops() {
		return environment->List.of();
	}

	@Override
	public DataFetcher<Object> station() {
		return environment -> null;
	}

	@Override
	public DataFetcher<Iterable<Object>> stations() {
		return environment->List.of();
	}

	@Override
	public DataFetcher<Iterable<Object>> routes() {
		return environment -> null;
	}

	@Override
	public DataFetcher<Object> route() {
		return environment -> null;
	}
	
	private Router getRouter(DataFetchingEnvironment environment) {
		return environment.<GraphQLRequestContext>getContext().getRouter();
	}

	private GraphIndex getGraphIndex(DataFetchingEnvironment environment) {
		return environment.<GraphQLRequestContext>getContext().getIndex();
	}

}
