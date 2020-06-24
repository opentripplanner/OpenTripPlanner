package org.opentripplanner.ext.legacygraphqlapi.datafetchers;

import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import org.opentripplanner.api.resource.DebugOutput;
import org.opentripplanner.ext.legacygraphqlapi.generated.LegacyGraphQLDataFetchers;

public class LegacyGraphQLDebugOutputImpl
    implements LegacyGraphQLDataFetchers.LegacyGraphQLDebugOutput {

  @Override
  public DataFetcher<Long> totalTime() {
    return environment -> getSource(environment).totalTime;
  }

  @Override
  public DataFetcher<Long> pathCalculationTime() {
    return environment -> getSource(environment).pathCalculationTime;
  }

  @Override
  public DataFetcher<Long> precalculationTime() {
    return environment -> getSource(environment).precalculationTime;
  }

  @Override
  public DataFetcher<Long> renderingTime() {
    return environment -> getSource(environment).renderingTime;
  }

  @Override
  public DataFetcher<Boolean> timedOut() {
    return environment -> getSource(environment).timedOut;
  }

  private DebugOutput getSource(DataFetchingEnvironment environment) {
    return environment.getSource();
  }
}
