package org.opentripplanner.apis.gtfs.datafetchers;

import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import java.util.stream.Collectors;
import org.opentripplanner.apis.gtfs.GraphQLRequestContext;
import org.opentripplanner.apis.gtfs.generated.GraphQLDataFetchers;
import org.opentripplanner.routing.core.FareComponent;
import org.opentripplanner.transit.model.network.Route;
import org.opentripplanner.transit.service.TransitService;

public class fareComponentImpl implements GraphQLDataFetchers.GraphQLFareComponent {

  @Override
  public DataFetcher<Integer> cents() {
    return environment -> getSource(environment).price().minorUnitAmount();
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
      TransitService transitService = getTransitService(environment);
      return getSource(environment)
        .routes()
        .stream()
        .map(transitService::getRouteForId)
        .collect(Collectors.toList());
    };
  }

  private TransitService getTransitService(DataFetchingEnvironment environment) {
    return environment.<GraphQLRequestContext>getContext().transitService();
  }

  private FareComponent getSource(DataFetchingEnvironment environment) {
    return environment.getSource();
  }
}
