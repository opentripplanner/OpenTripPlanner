package org.opentripplanner.ext.legacygraphqlapi.datafetchers;

import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import org.opentripplanner.ext.legacygraphqlapi.LegacyGraphQLRequestContext;
import org.opentripplanner.ext.legacygraphqlapi.generated.LegacyGraphQLDataFetchers;
import org.opentripplanner.transit.service.TransitService;

public class LegacyGraphQLserviceTimeRangeImpl
  implements LegacyGraphQLDataFetchers.LegacyGraphQLServiceTimeRange {

  @Override
  public DataFetcher<Long> end() {
    return environment -> getTransitService(environment).getTransitServiceEnds().toEpochSecond();
  }

  @Override
  public DataFetcher<Long> start() {
    return environment -> getTransitService(environment).getTransitServiceStarts().toEpochSecond();
  }

  private TransitService getTransitService(DataFetchingEnvironment environment) {
    return environment.<LegacyGraphQLRequestContext>getContext().transitService();
  }
}
