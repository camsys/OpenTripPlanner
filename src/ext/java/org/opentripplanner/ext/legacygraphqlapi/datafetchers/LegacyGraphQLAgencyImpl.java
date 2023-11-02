package org.opentripplanner.ext.legacygraphqlapi.datafetchers;


import graphql.relay.Relay;
import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import org.opentripplanner.ext.legacygraphqlapi.LegacyGraphQLRequestContext;
import org.opentripplanner.ext.legacygraphqlapi.generated.LegacyGraphQLDataFetchers;
import org.opentripplanner.transit.model.organization.Agency;
import org.opentripplanner.transit.model.network.Route;
import org.opentripplanner.routing.RoutingService;
import org.opentripplanner.routing.alertpatch.TransitAlert;

import java.util.List;
import java.util.stream.Collectors;

public class LegacyGraphQLAgencyImpl implements LegacyGraphQLDataFetchers.LegacyGraphQLAgency {

  @Override
  public DataFetcher<Relay.ResolvedGlobalId> id() {
    return environment -> new Relay.ResolvedGlobalId("Agency",
        getSource(environment).getId().toString()
    );
  }

  @Override
  public DataFetcher<String> gtfsId() {
    return environment -> getSource(environment).getId().toString();
  }

  @Override
  public DataFetcher<String> name() {
    return environment -> getSource(environment).getName();
  }

  @Override
  public DataFetcher<String> url() {
    return environment -> getSource(environment).getUrl();
  }

  @Override
  public DataFetcher<String> timezone() {
    return environment -> getSource(environment).getTimezone();
  }

  @Override
  public DataFetcher<String> lang() {
    return environment -> getSource(environment).getLang();
  }

  @Override
  public DataFetcher<String> phone() {
    return environment -> getSource(environment).getPhone();
  }

  @Override
  public DataFetcher<String> fareUrl() {
    return environment -> getSource(environment).getFareUrl();
  }

  @Override
  public DataFetcher<Iterable<Route>> routes() {
    return environment -> getRoutingService(environment)
        .getAllRoutes()
        .stream()
        .filter(route -> route.getAgency().equals(getSource(environment)))
        .collect(Collectors.toList());
  }

  @Override
  //TODO
  public DataFetcher<Iterable<TransitAlert>> alerts() {
    return environment -> List.of();
  }

  private RoutingService getRoutingService(DataFetchingEnvironment environment) {
    return environment.<LegacyGraphQLRequestContext>getContext().getRoutingService();
  }

  private Agency getSource(DataFetchingEnvironment environment) {
    return environment.getSource();
  }
}
