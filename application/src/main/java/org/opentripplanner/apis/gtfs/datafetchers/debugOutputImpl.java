package org.opentripplanner.apis.gtfs.datafetchers;

import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import org.opentripplanner.api.resource.DebugOutput;
import org.opentripplanner.apis.gtfs.generated.GraphQLDataFetchers;

public class debugOutputImpl implements GraphQLDataFetchers.GraphQLDebugOutput {

  private static final long NANOS_TO_MILLIS = 1000000;

  @Override
  public DataFetcher<Long> pathCalculationTime() {
    return environment -> getSource(environment).transitRouterTime / NANOS_TO_MILLIS;
  }

  @Override
  public DataFetcher<Long> precalculationTime() {
    return environment -> getSource(environment).precalculationTime / NANOS_TO_MILLIS;
  }

  @Override
  public DataFetcher<Long> renderingTime() {
    return environment -> getSource(environment).renderingTime / NANOS_TO_MILLIS;
  }

  @Override
  public DataFetcher<Boolean> timedOut() {
    return environment -> false;
  }

  @Override
  public DataFetcher<Long> totalTime() {
    return environment -> getSource(environment).totalTime / NANOS_TO_MILLIS;
  }

  private DebugOutput getSource(DataFetchingEnvironment environment) {
    return environment.getSource();
  }
}
