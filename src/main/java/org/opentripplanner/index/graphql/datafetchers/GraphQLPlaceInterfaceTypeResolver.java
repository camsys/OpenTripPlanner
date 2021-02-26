package org.opentripplanner.index.graphql.datafetchers;

import graphql.TypeResolutionEnvironment;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLSchema;
import graphql.schema.TypeResolver;

import org.onebusaway.gtfs.model.Stop;

public class GraphQLPlaceInterfaceTypeResolver implements TypeResolver {

	@Override
	public GraphQLObjectType getType(TypeResolutionEnvironment environment) {
		Object o = environment.getObject();
		GraphQLSchema schema = environment.getSchema();

		if (o instanceof Stop) return schema.getObjectType("Stop");

		return null;
	}
}
