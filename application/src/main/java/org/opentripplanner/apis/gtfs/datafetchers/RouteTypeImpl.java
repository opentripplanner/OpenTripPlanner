package org.opentripplanner.apis.gtfs.datafetchers;

import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import java.util.stream.Collectors;
import org.opentripplanner.apis.gtfs.GraphQLRequestContext;
import org.opentripplanner.apis.gtfs.generated.GraphQLDataFetchers;
import org.opentripplanner.apis.gtfs.model.RouteTypeModel;
import org.opentripplanner.transit.model.network.Route;
import org.opentripplanner.transit.model.organization.Agency;
import org.opentripplanner.transit.service.TransitService;

public class RouteTypeImpl implements GraphQLDataFetchers.GraphQLRouteType {

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
        .listRoutes()
        .stream()
        .filter(
          route ->
            route.getId().getFeedId().equals(getSource(environment).getFeedId()) &&
            route.getGtfsType() == getSource(environment).getRouteType() &&
            (agency == null || route.getAgency().equals(agency))
        )
        .collect(Collectors.toList());
    };
  }

  private TransitService getTransitService(DataFetchingEnvironment environment) {
    return environment.<GraphQLRequestContext>getContext().transitService();
  }

  private RouteTypeModel getSource(DataFetchingEnvironment environment) {
    return environment.getSource();
  }
}
