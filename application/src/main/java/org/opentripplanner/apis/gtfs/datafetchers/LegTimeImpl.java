package org.opentripplanner.apis.gtfs.datafetchers;

import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import java.time.OffsetDateTime;
import org.opentripplanner.apis.gtfs.generated.GraphQLDataFetchers;
import org.opentripplanner.model.plan.leg.LegCallTime;
import org.opentripplanner.model.plan.leg.LegRealTimeEstimate;

public class LegTimeImpl implements GraphQLDataFetchers.GraphQLLegTime {

  @Override
  public DataFetcher<LegRealTimeEstimate> estimated() {
    return environment -> getSource(environment).estimated();
  }

  @Override
  public DataFetcher<OffsetDateTime> scheduledTime() {
    return environment -> getSource(environment).scheduledTime().toOffsetDateTime();
  }

  private LegCallTime getSource(DataFetchingEnvironment environment) {
    return environment.getSource();
  }
}
