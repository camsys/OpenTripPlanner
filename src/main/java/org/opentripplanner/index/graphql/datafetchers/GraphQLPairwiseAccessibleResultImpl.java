package org.opentripplanner.index.graphql.datafetchers;

import graphql.schema.DataFetcher;
import java.util.stream.Collectors;

import org.opentripplanner.api.model.PairwiseAccessibilityShort;
import org.opentripplanner.index.graphql.generated.GraphQLDataFetchers;

public class GraphQLPairwiseAccessibleResultImpl implements GraphQLDataFetchers.GraphQLPairwiseAccessibleResult {

	@Override
	public DataFetcher<Object> to() {
		return environment -> {
			PairwiseAccessibilityShort e = environment.getSource();
	    	return e.from;
	    };
	}

	@Override
	public DataFetcher<Object> from() {
		return environment -> {
			PairwiseAccessibilityShort e = environment.getSource();
	    	return e.to;
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
	
}
