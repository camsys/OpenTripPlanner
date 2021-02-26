package org.opentripplanner.index.graphql.datafetchers;

import graphql.TypeResolutionEnvironment;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLSchema;
import graphql.schema.TypeResolver;
import org.opentripplanner.routing.bike_park.BikePark;
import org.opentripplanner.routing.bike_rental.BikeRentalStation;

public class GraphQLPlaceInterfaceTypeResolver implements TypeResolver {

  @Override
  public GraphQLObjectType getType(TypeResolutionEnvironment environment) {

    return null;
  }
}
