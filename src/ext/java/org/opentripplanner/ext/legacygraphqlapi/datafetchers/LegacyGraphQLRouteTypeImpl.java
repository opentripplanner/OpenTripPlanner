package org.opentripplanner.ext.legacygraphqlapi.datafetchers;

import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import java.util.stream.Collectors;
import org.opentripplanner.ext.legacygraphqlapi.LegacyGraphQLRequestContext;
import org.opentripplanner.ext.legacygraphqlapi.generated.LegacyGraphQLDataFetchers;
import org.opentripplanner.ext.legacygraphqlapi.model.LegacyGraphQLRouteTypeModel;
import org.opentripplanner.transit.model.network.Route;
import org.opentripplanner.transit.model.organization.Agency;
import org.opentripplanner.transit.service.TransitService;

public class LegacyGraphQLRouteTypeImpl
  implements LegacyGraphQLDataFetchers.LegacyGraphQLRouteType {

  @Override
  public DataFetcher<Agency> agency() {
    return environment -> getSource(environment).getAgency();
  }

  @Override
  public DataFetcher<Integer> routeType() {
    return environment -> getSource(environment).getRouteType();
  }

  @Override
  public DataFetcher<Iterable<Route>> routes() {
    return environment -> {
      Agency agency = getSource(environment).getAgency();
      return getTransitService(environment)
        .getAllRoutes()
        .stream()
        .filter(route ->
          route.getId().getFeedId().equals(getSource(environment).getFeedId()) &&
          route.getGtfsType() == getSource(environment).getRouteType() &&
          (agency == null || route.getAgency().equals(agency))
        )
        .collect(Collectors.toList());
    };
  }

  private TransitService getTransitService(DataFetchingEnvironment environment) {
    return environment.<LegacyGraphQLRequestContext>getContext().transitService();
  }

  private LegacyGraphQLRouteTypeModel getSource(DataFetchingEnvironment environment) {
    return environment.getSource();
  }
}
