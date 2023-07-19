package org.opentripplanner.ext.gtfsgraphqlapi.datafetchers;

import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import org.opentripplanner.ext.gtfsgraphqlapi.generated.GraphQLDataFetchers;
import org.opentripplanner.ext.gtfsgraphqlapi.model.StopOnRouteModel;
import org.opentripplanner.transit.model.network.Route;

public class StopOnRouteImpl implements GraphQLDataFetchers.GraphQLStopOnRoute {

  @Override
  public DataFetcher<Route> route() {
    return environment -> getSource(environment).getRoute();
  }

  @Override
  public DataFetcher<Object> stop() {
    return environment -> getSource(environment).getStop();
  }

  private StopOnRouteModel getSource(DataFetchingEnvironment environment) {
    return environment.getSource();
  }
}
