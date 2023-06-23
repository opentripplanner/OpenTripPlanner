package org.opentripplanner.ext.gtfsgraphqlapi.datafetchers;

import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import org.opentripplanner.ext.gtfsgraphqlapi.generated.LegacyGraphQLDataFetchers;
import org.opentripplanner.ext.gtfsgraphqlapi.model.LegacyGraphQLStopOnRouteModel;
import org.opentripplanner.transit.model.network.Route;

public class StopOnRouteImpl implements LegacyGraphQLDataFetchers.LegacyGraphQLStopOnRoute {

  @Override
  public DataFetcher<Route> route() {
    return environment -> getSource(environment).getRoute();
  }

  @Override
  public DataFetcher<Object> stop() {
    return environment -> getSource(environment).getStop();
  }

  private LegacyGraphQLStopOnRouteModel getSource(DataFetchingEnvironment environment) {
    return environment.getSource();
  }
}
