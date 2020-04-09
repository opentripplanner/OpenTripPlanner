package org.opentripplanner.ext.legacygraphqlapi.datafetchers;


import graphql.relay.Relay;
import graphql.schema.DataFetcher;
import org.opentripplanner.ext.legacygraphqlapi.LegacyGraphQLRequestContext;
import org.opentripplanner.model.Agency;
import org.opentripplanner.model.Route;
import org.opentripplanner.routing.alertpatch.Alert;

import java.util.Collections;
import java.util.stream.Collectors;

public class LegacyGraphQLAgencyImpl implements LegacyGraphQLDataFetchers.LegacyGraphQLAgency {

  @Override
  // TODO
  public DataFetcher<Relay.ResolvedGlobalId> id() {
    return environment -> new Relay.ResolvedGlobalId("Agency",
        environment.<Agency>getSource().getId()
    );
  }

  @Override
  public DataFetcher<String> gtfsId() {
    return environment -> environment.<Agency>getSource().getId();
  }

  @Override
  public DataFetcher<String> name() {
    return environment -> environment.<Agency>getSource().getName();
  }

  @Override
  public DataFetcher<String> url() {
    return environment -> environment.<Agency>getSource().getUrl();
  }

  @Override
  public DataFetcher<String> timezone() {
    return environment -> environment.<Agency>getSource().getTimezone();
  }

  @Override
  public DataFetcher<String> lang() {
    return environment -> environment.<Agency>getSource().getLang();
  }

  @Override
  public DataFetcher<String> phone() {
    return environment -> environment.<Agency>getSource().getPhone();
  }

  @Override
  public DataFetcher<String> fareUrl() {
    return environment -> environment.<Agency>getSource().getFareUrl();
  }

  @Override
  public DataFetcher<Iterable<Route>> routes() {
    return environment -> environment.<LegacyGraphQLRequestContext>getContext()
        .getRoutingService()
        .getRouteForId()
        .values()
        .stream()
        .filter(route -> route.getAgency().equals(environment.getSource()))
        .collect(Collectors.toList());
  }

  @Override
  //TODO
  public DataFetcher<Iterable<Alert>> alerts() {
    return environment -> Collections.emptyList();
  }
}
