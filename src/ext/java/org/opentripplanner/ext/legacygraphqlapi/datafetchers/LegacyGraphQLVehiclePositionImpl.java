package org.opentripplanner.ext.legacygraphqlapi.datafetchers;

import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import org.opentripplanner.ext.legacygraphqlapi.generated.LegacyGraphQLDataFetchers.LegacyGraphQLVehiclePosition;
import org.opentripplanner.model.vehicle_position.RealtimeVehiclePosition;

public class LegacyGraphQLVehiclePositionImpl implements LegacyGraphQLVehiclePosition {


    @Override
    public DataFetcher<Double> lat() {
        return env -> getSource(env).lat;
    }

    @Override
    public DataFetcher<Double> lon() {
        return env -> getSource(env).lon;
    }

    private RealtimeVehiclePosition getSource(DataFetchingEnvironment environment) {
        return environment.getSource();
    }
}
