package org.opentripplanner.apis.gtfs.datafetchers;

import graphql.relay.Relay;
import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import org.opentripplanner.apis.gtfs.GraphQLRequestContext;
import org.opentripplanner.apis.gtfs.generated.GraphQLDataFetchers;
import org.opentripplanner.routing.graphfinder.NearbyStop;
import org.opentripplanner.transit.service.TransitService;

public class stopAtDistanceImpl implements GraphQLDataFetchers.GraphQLStopAtDistance {

  @Override
  public DataFetcher<Integer> distance() {
    return environment -> (int) getSource(environment).distance;
  }

  @Override
  public DataFetcher<Relay.ResolvedGlobalId> id() {
    return environment ->
      new Relay.ResolvedGlobalId(
        "stopAtDistance",
        getSource(environment).distance + ";" + getSource(environment).stopId.toString()
      );
  }

  @Override
  public DataFetcher<Object> stop() {
    return environment ->
      getTransitService(environment).getStopLocation(getSource(environment).stopId);
  }

  private TransitService getTransitService(DataFetchingEnvironment environment) {
    return environment.<GraphQLRequestContext>getContext().transitService();
  }

  private NearbyStop getSource(DataFetchingEnvironment environment) {
    return environment.getSource();
  }
}
