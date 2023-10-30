package org.opentripplanner.apis.gtfs.datafetchers;

import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import org.opentripplanner.apis.gtfs.generated.GraphQLDataFetchers;
import org.opentripplanner.apis.gtfs.generated.GraphQLTypes;
import org.opentripplanner.service.realtimevehicles.model.RealtimeVehicle;

public class StopRelationshipImpl implements GraphQLDataFetchers.GraphQLStopRelationship {

  @Override
  public DataFetcher<Object> status() {
    return env ->
      switch (getSource(env).status()) {
        case INCOMING_AT -> GraphQLTypes.GraphQLVehicleStopStatus.INCOMING_AT;
        case IN_TRANSIT_TO -> GraphQLTypes.GraphQLVehicleStopStatus.IN_TRANSIT_TO;
        case STOPPED_AT -> GraphQLTypes.GraphQLVehicleStopStatus.STOPPED_AT;
      };
  }

  @Override
  public DataFetcher<Object> stop() {
    return env -> getSource(env).stop();
  }

  private RealtimeVehicle.StopRelationship getSource(DataFetchingEnvironment environment) {
    return environment.getSource();
  }
}
