package org.opentripplanner.apis.gtfs.datafetchers;

import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import java.time.Duration;
import java.time.OffsetDateTime;
import org.opentripplanner.apis.gtfs.generated.GraphQLDataFetchers;
import org.opentripplanner.model.plan.leg.LegRealTimeEstimate;

public class RealTimeEstimateImpl implements GraphQLDataFetchers.GraphQLRealTimeEstimate {

  @Override
  public DataFetcher<Duration> delay() {
    return environment -> getSource(environment).delay();
  }

  @Override
  public DataFetcher<OffsetDateTime> time() {
    return environment -> getSource(environment).time().toOffsetDateTime();
  }

  private LegRealTimeEstimate getSource(DataFetchingEnvironment environment) {
    return environment.getSource();
  }
}
