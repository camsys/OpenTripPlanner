package org.opentripplanner.index.graphql.datafetchers;

import graphql.relay.Relay;
import graphql.relay.Relay.ResolvedGlobalId;
import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import java.util.List;
import org.onebusaway.gtfs.model.Agency;
import org.opentripplanner.index.graphql.GraphQLRequestContext;
import org.opentripplanner.index.graphql.generated.GraphQLDataFetchers;
import org.opentripplanner.routing.graph.GraphIndex;
import org.opentripplanner.standalone.Router;

public class GraphQLAgencyImpl implements GraphQLDataFetchers.GraphQLAgency {

	@Override
	public DataFetcher<ResolvedGlobalId> id() {
	    return environment -> {
	    	Agency e = environment.getSource();
	    	return new Relay.ResolvedGlobalId("Agency", e.getId());
	    };
	}

	@Override
	public DataFetcher<String> gtfsId() {
	    return environment -> {
	    	Agency e = environment.getSource();
	    	return e.getId();
	    };
	}

	@Override
	public DataFetcher<String> name() {
	    return environment -> {
	    	Agency e = environment.getSource();
	    	return e.getName();
	    };
	}

	@Override
	public DataFetcher<String> url() {
	    return environment -> {
	    	Agency e = environment.getSource();
	    	return e.getUrl();
	    };
	}

	@Override
	public DataFetcher<String> timezone() {
	    return environment -> {
	    	Agency e = environment.getSource();
	    	return e.getTimezone();
	    };
	}

	@Override
	public DataFetcher<String> lang() {
	    return environment -> {
	    	Agency e = environment.getSource();
	    	return e.getLang();
	    };
	}

	@Override
	public DataFetcher<String> phone() {
	    return environment -> {
	    	Agency e = environment.getSource();
	    	return e.getPhone();
	    };
	}

	@Override
	public DataFetcher<String> fareUrl() {
	    return environment -> {
	    	Agency e = environment.getSource();
	    	return e.getFareUrl();
	    };
	}

	@Override
	public DataFetcher<Iterable<Object>> routes() {
		return environment -> List.of("__NOT IMPLEMENTED__");
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
