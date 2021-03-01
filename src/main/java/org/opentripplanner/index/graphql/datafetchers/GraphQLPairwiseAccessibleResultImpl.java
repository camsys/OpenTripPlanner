package org.opentripplanner.index.graphql.datafetchers;

import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;

import java.util.stream.Collectors;

import org.opentripplanner.api.model.PairwiseAccessibilityShort;
import org.opentripplanner.index.graphql.GraphQLRequestContext;
import org.opentripplanner.index.graphql.generated.GraphQLDataFetchers;
import org.opentripplanner.routing.graph.GraphIndex;

public class GraphQLPairwiseAccessibleResultImpl implements GraphQLDataFetchers.GraphQLPairwiseAccessibleResult {

	@Override
	public DataFetcher<Object> to() {
		return environment -> {
			PairwiseAccessibilityShort e = environment.getSource();
	    	return getGraphIndex(environment).stopForId.get(e.to.id);
	    };
	}

	@Override
	public DataFetcher<Iterable<String>> dependsOnEquipment() {
		return environment -> {
			PairwiseAccessibilityShort e = environment.getSource();
	    	return e.dependsOnEquipment;
	    };
	}
	
	@Override
	public DataFetcher<Boolean> isCurrentlyAccessible() {
		return environment -> {
			PairwiseAccessibilityShort e = environment.getSource();
	    	return e.isCurrentlyAccessible;
	    };
	}
	
	@Override
	public DataFetcher<Iterable<Object>> alerts() {
		return environment -> {
			PairwiseAccessibilityShort e = environment.getSource();
	    	return e.alerts.parallelStream().collect(Collectors.toList());
	    };
	}

	private GraphIndex getGraphIndex(DataFetchingEnvironment environment) {
		return environment.<GraphQLRequestContext>getContext().getIndex();
	}
	
}
