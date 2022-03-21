package org.opentripplanner.ext.legacygraphqlapi.datafetchers;

import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import org.opentripplanner.ext.legacygraphqlapi.generated.LegacyGraphQLDataFetchers.LegacyGraphQLVehicleStop;
import org.opentripplanner.ext.legacygraphqlapi.generated.LegacyGraphQLTypes.LegacyGraphQLVehicleStopStatus;
import org.opentripplanner.model.vehicle_position.RealtimeVehiclePosition.VehicleStop;

public class LegacyGraphQLVehicleStopImpl implements LegacyGraphQLVehicleStop {

    @Override
    public DataFetcher<Object> status() {
        return env -> switch (getSource(env).status()) {
            case INCOMING_AT -> LegacyGraphQLVehicleStopStatus.INCOMING_AT;
            case IN_TRANSIT_TO -> LegacyGraphQLVehicleStopStatus.IN_TRANSIT_TO;
            case STOPPED_AT -> LegacyGraphQLVehicleStopStatus.STOPPED_AT;
        };
    }

    @Override
    public DataFetcher<Object> stop() {
        return env -> getSource(env).stop();
    }

    private VehicleStop getSource(DataFetchingEnvironment environment) {
        return environment.getSource();
    }
}
