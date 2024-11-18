package org.opentripplanner.apis.gtfs.datafetchers;

import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import java.time.OffsetDateTime;
import org.opentripplanner.apis.gtfs.generated.GraphQLDataFetchers;
import org.opentripplanner.transit.model.timetable.CallRealTimeEstimate;
import org.opentripplanner.transit.model.timetable.CallTime;

public class CallTimeImpl implements GraphQLDataFetchers.GraphQLCallTime {

  @Override
  public DataFetcher<CallRealTimeEstimate> estimated() {
    return environment -> getSource(environment).estimated();
  }

  @Override
  public DataFetcher<OffsetDateTime> scheduledTime() {
    return environment -> getSource(environment).scheduledTime().toOffsetDateTime();
  }

  private CallTime getSource(DataFetchingEnvironment environment) {
    return environment.getSource();
  }
}
