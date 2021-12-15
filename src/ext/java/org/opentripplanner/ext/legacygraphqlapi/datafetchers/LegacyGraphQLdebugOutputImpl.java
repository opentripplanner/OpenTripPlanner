package org.opentripplanner.ext.legacygraphqlapi.datafetchers;

import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import org.opentripplanner.api.resource.DebugOutput;
import org.opentripplanner.ext.legacygraphqlapi.generated.LegacyGraphQLDataFetchers;

public class LegacyGraphQLdebugOutputImpl
    implements LegacyGraphQLDataFetchers.LegacyGraphQLDebugOutput {

  private static final long nanosToMillis = 1000000;

  @Override
  public DataFetcher<Long> totalTime() {
    return environment -> getSource(environment).totalTime / nanosToMillis;
  }

  @Override
  public DataFetcher<Long> pathCalculationTime() {
    return environment -> getSource(environment).transitRouterTime / nanosToMillis;
  }

  @Override
  public DataFetcher<Long> precalculationTime() {
    return environment -> getSource(environment).precalculationTime / nanosToMillis;
  }

  @Override
  public DataFetcher<Long> renderingTime() {
    return environment -> getSource(environment).renderingTime / nanosToMillis;
  }

  @Override
  public DataFetcher<Boolean> timedOut() {
    return environment -> false;
  }

  private DebugOutput getSource(DataFetchingEnvironment environment) {
    return environment.getSource();
  }
}
