package org.opentripplanner.index.graphql.datafetchers;

import graphql.relay.Relay;
import graphql.relay.Relay.ResolvedGlobalId;
import graphql.schema.DataFetcher;
import java.util.stream.Collectors;

import org.opentripplanner.index.graphql.generated.GraphQLDataFetchers;
import org.opentripplanner.index.model.EquipmentShort;

public class GraphQLEquipmentImpl implements GraphQLDataFetchers.GraphQLEquipment {

	@Override
	public DataFetcher<ResolvedGlobalId> id() {
	    return environment -> {
	    	EquipmentShort e = environment.getSource();
	    	return new Relay.ResolvedGlobalId("Equipment", e.equipmentId);
	    };
	}

	@Override
	public DataFetcher<String> mtaEquipmentId() {
		return environment -> {
	    	EquipmentShort e = environment.getSource();
	    	return e.equipmentId;
	    };
	}

	@Override
	public DataFetcher<Boolean> isCurrentlyAccessible() {
		return environment -> {
	    	EquipmentShort e = environment.getSource();
	    	return e.isCurrentlyAccessible;
	    };
	}
	
	@Override
	public DataFetcher<Iterable<Object>> alerts() {
		return environment -> {
	    	EquipmentShort e = environment.getSource();
	    	return e.alerts.parallelStream().collect(Collectors.toList());
	    };
	}
	
}
