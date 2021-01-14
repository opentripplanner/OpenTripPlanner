package org.opentripplanner.ext.legacygraphqlapi.datafetchers;

import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import org.opentripplanner.ext.legacygraphqlapi.LegacyGraphQLRequestContext;
import org.opentripplanner.ext.legacygraphqlapi.generated.LegacyGraphQLDataFetchers;
import org.opentripplanner.model.Agency;
import org.opentripplanner.routing.RoutingService;

import java.util.stream.Collectors;

public class LegacyGraphQLFeedImpl implements LegacyGraphQLDataFetchers.LegacyGraphQLFeed {

  @Override
  public DataFetcher<String> feedId() {
    return this::getSource;
  }

  @Override
  public DataFetcher<Iterable<Agency>> agencies() {
    return environment -> {
      String id = getSource(environment);
      return getRoutingService(environment)
          .getAgencies()
          .stream()
          .filter(agency -> agency.getId().getFeedId().equals(id))
          .collect(Collectors.toList());
    };
  }

  private RoutingService getRoutingService(DataFetchingEnvironment environment) {
    return environment.<LegacyGraphQLRequestContext>getContext().getRoutingService();
  }

  private String getSource(DataFetchingEnvironment environment) {
    return environment.getSource();
  }
}
