package org.opentripplanner.ext.gtfsgraphqlapi.datafetchers;

import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import org.opentripplanner.ext.gtfsgraphqlapi.generated.GraphQLDataFetchers.GraphQLStopRelationship;
import org.opentripplanner.ext.gtfsgraphqlapi.generated.GraphQLTypes.GraphQLVehicleStopStatus;
import org.opentripplanner.service.vehiclepositions.model.RealtimeVehiclePosition.StopRelationship;

public class StopRelationshipImpl implements GraphQLStopRelationship {

  @Override
  public DataFetcher<Object> status() {
    return env ->
      switch (getSource(env).status()) {
        case INCOMING_AT -> GraphQLVehicleStopStatus.INCOMING_AT;
        case IN_TRANSIT_TO -> GraphQLVehicleStopStatus.IN_TRANSIT_TO;
        case STOPPED_AT -> GraphQLVehicleStopStatus.STOPPED_AT;
      };
  }

  @Override
  public DataFetcher<Object> stop() {
    return env -> getSource(env).stop();
  }

  private StopRelationship getSource(DataFetchingEnvironment environment) {
    return environment.getSource();
  }
}
