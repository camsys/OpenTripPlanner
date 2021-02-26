package org.opentripplanner.index.graphql.datafetchers;

import graphql.TypeResolutionEnvironment;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLSchema;
import graphql.schema.TypeResolver;

import org.onebusaway.gtfs.model.Agency;
import org.onebusaway.gtfs.model.Route;
import org.onebusaway.gtfs.model.Stop;
import org.opentripplanner.routing.alertpatch.AlertPatch;

import com.conveyal.gtfs.model.FeedInfo;

public class GraphQLNodeTypeResolver implements TypeResolver {

	@Override
	public GraphQLObjectType getType(TypeResolutionEnvironment environment) {
		Object o = environment.getObject();
		GraphQLSchema schema = environment.getSchema();

		if (o instanceof Agency) return schema.getObjectType("Agency");
		if (o instanceof AlertPatch) return schema.getObjectType("Alert");
		if (o instanceof FeedInfo) return schema.getObjectType("Feed");
		if (o instanceof Route) return schema.getObjectType("Route");
		if (o instanceof Stop) return schema.getObjectType("Stop");
		return null;
	}
}
