package org.opentripplanner.ext.legacygraphqlapi.datafetchers;

import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import java.util.stream.Collectors;
import org.opentripplanner.ext.legacygraphqlapi.LegacyGraphQLRequestContext;
import org.opentripplanner.ext.legacygraphqlapi.generated.LegacyGraphQLDataFetchers;
import org.opentripplanner.transit.model.network.Route;
import org.opentripplanner.routing.RoutingService;
import org.opentripplanner.routing.core.FareComponent;
//import org.opentripplanner.transit.service.TransitService;

public class LegacyGraphQLfareComponentImpl
  implements LegacyGraphQLDataFetchers.LegacyGraphQLFareComponent {

  @Override
  public DataFetcher<Integer> cents() {
    return environment -> getSource(environment).price().cents();
  }

  @Override
  public DataFetcher<String> currency() {
    return environment -> getSource(environment).price().currency().getCurrencyCode();
  }

  @Override
  public DataFetcher<String> fareId() {
    return environment -> getSource(environment).fareId().toString();
  }

  @Override
  public DataFetcher<Iterable<Route>> routes() {
    return environment -> {
      RoutingService routingService = getRoutingService(environment);
      return getSource(environment).routes
          .stream()
          .map(routingService::getRouteForId)
          .collect(Collectors.toList());
    };
  }

  private RoutingService getRoutingService(DataFetchingEnvironment environment) {
    return environment.<LegacyGraphQLRequestContext>getContext().getRoutingService();
  }

  private FareComponent getSource(DataFetchingEnvironment environment) {
    return environment.getSource();
  }
}
