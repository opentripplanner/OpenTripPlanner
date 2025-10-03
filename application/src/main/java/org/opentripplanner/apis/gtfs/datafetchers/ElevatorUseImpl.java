package org.opentripplanner.apis.gtfs.datafetchers;

import graphql.schema.DataFetcher;
import org.opentripplanner.apis.gtfs.generated.GraphQLDataFetchers;
import org.opentripplanner.model.plan.walkstep.verticaltransportationuse.ElevatorUse;

public class ElevatorUseImpl implements GraphQLDataFetchers.GraphQLElevatorUse {

  @Override
  public DataFetcher<String> toLevelName() {
    return environment -> {
      ElevatorUse elevatorUse = environment.getSource();
      return elevatorUse.toLevelName();
    };
  }
}
