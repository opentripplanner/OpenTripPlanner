package org.opentripplanner.apis.gtfs.datafetchers;

import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import org.opentripplanner.apis.gtfs.GraphQLRequestContext;
import org.opentripplanner.apis.gtfs.generated.GraphQLDataFetchers;
import org.opentripplanner.transit.service.TransitService;

public class serviceTimeRangeImpl implements GraphQLDataFetchers.GraphQLServiceTimeRange {

  @Override
  public DataFetcher<Long> end() {
    return environment -> getTransitService(environment).getTransitServiceEnds().toEpochSecond();
  }

  @Override
  public DataFetcher<Long> start() {
    return environment -> getTransitService(environment).getTransitServiceStarts().toEpochSecond();
  }

  private TransitService getTransitService(DataFetchingEnvironment environment) {
    return environment.<GraphQLRequestContext>getContext().transitService();
  }
}
