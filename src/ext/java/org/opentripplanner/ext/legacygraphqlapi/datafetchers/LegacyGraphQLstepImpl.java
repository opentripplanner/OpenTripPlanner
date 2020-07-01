package org.opentripplanner.ext.legacygraphqlapi.datafetchers;

import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import org.opentripplanner.common.model.P2;
import org.opentripplanner.ext.legacygraphqlapi.generated.LegacyGraphQLDataFetchers;
import org.opentripplanner.model.plan.WalkStep;

public class LegacyGraphQLstepImpl implements LegacyGraphQLDataFetchers.LegacyGraphQLStep {

  @Override
  public DataFetcher<Double> distance() {
    return environment -> getSource(environment).distance;
  }

  @Override
  public DataFetcher<Double> lon() {
    return environment -> getSource(environment).startLocation.longitude();
  }

  @Override
  public DataFetcher<Double> lat() {
    return environment -> getSource(environment).startLocation.latitude();
  }

  @Override
  public DataFetcher<Iterable<P2<Double>>> elevationProfile() {
    return environment -> getSource(environment).elevation;
  }

  private WalkStep getSource(DataFetchingEnvironment environment) {
    return environment.getSource();
  }
}
