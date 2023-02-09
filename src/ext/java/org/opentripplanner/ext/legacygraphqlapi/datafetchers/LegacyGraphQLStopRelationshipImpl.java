package org.opentripplanner.ext.legacygraphqlapi.datafetchers;

import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import org.opentripplanner.ext.legacygraphqlapi.generated.LegacyGraphQLDataFetchers.LegacyGraphQLStopRelationship;
import org.opentripplanner.ext.legacygraphqlapi.generated.LegacyGraphQLTypes.LegacyGraphQLVehicleStopStatus;
import org.opentripplanner.service.vehiclepositions.model.RealtimeVehiclePosition.StopRelationship;

public class LegacyGraphQLStopRelationshipImpl implements LegacyGraphQLStopRelationship {

  @Override
  public DataFetcher<Object> status() {
    return env ->
      switch (getSource(env).status()) {
        case INCOMING_AT -> LegacyGraphQLVehicleStopStatus.INCOMING_AT;
        case IN_TRANSIT_TO -> LegacyGraphQLVehicleStopStatus.IN_TRANSIT_TO;
        case STOPPED_AT -> LegacyGraphQLVehicleStopStatus.STOPPED_AT;
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
