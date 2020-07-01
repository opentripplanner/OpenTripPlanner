package org.opentripplanner.ext.legacygraphqlapi.datafetchers;

import graphql.relay.Relay;
import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import org.opentripplanner.ext.legacygraphqlapi.generated.LegacyGraphQLDataFetchers;
import org.opentripplanner.graph_builder.module.NearbyStopFinder;

public class LegacyGraphQLstopAtDistanceImpl
    implements LegacyGraphQLDataFetchers.LegacyGraphQLStopAtDistance {

  @Override
  public DataFetcher<Relay.ResolvedGlobalId> id() {
    return environment -> new Relay.ResolvedGlobalId("stopAtDistance",
        getSource(environment).distance + ";" + getSource(environment).tstop.getId().toString());
  }

  @Override
  public DataFetcher<Object> stop() {
    return environment -> getSource(environment).tstop;
  }

  @Override
  public DataFetcher<Integer> distance() {
    return environment -> (int) getSource(environment).distance;
  }

  private NearbyStopFinder.StopAtDistance getSource(DataFetchingEnvironment environment) {
    return environment.getSource();
  }
}
