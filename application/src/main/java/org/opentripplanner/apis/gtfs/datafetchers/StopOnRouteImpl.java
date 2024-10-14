package org.opentripplanner.apis.gtfs.datafetchers;

import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import org.opentripplanner.apis.gtfs.generated.GraphQLDataFetchers;
import org.opentripplanner.apis.gtfs.model.StopOnRouteModel;
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
