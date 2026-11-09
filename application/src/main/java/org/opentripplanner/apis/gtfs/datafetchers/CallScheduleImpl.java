package org.opentripplanner.apis.gtfs.datafetchers;

import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import org.opentripplanner.apis.gtfs.generated.GraphQLDataFetchers;
import org.opentripplanner.apis.gtfs.generated.GraphQLTypes.GraphQLPickupDropoffType;
import org.opentripplanner.apis.gtfs.mapping.PickDropMapper;
import org.opentripplanner.apis.gtfs.model.CallSchedule;
import org.opentripplanner.apis.gtfs.model.CallScheduledTime;

public class CallScheduleImpl implements GraphQLDataFetchers.GraphQLCallSchedule {

  @Override
  public DataFetcher<GraphQLPickupDropoffType> dropOffType() {
    return environment -> PickDropMapper.map(getSource(environment).dropOffType());
  }

  @Override
  public DataFetcher<GraphQLPickupDropoffType> pickupType() {
    return environment -> PickDropMapper.map(getSource(environment).pickupType());
  }

  @Override
  public DataFetcher<CallScheduledTime> time() {
    return environment -> getSource(environment).time();
  }

  private CallSchedule getSource(DataFetchingEnvironment environment) {
    return environment.getSource();
  }
}
