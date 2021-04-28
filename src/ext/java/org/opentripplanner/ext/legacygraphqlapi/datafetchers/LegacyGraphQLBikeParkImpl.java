package org.opentripplanner.ext.legacygraphqlapi.datafetchers;

import graphql.relay.Relay;
import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import org.opentripplanner.ext.legacygraphqlapi.generated.LegacyGraphQLDataFetchers;
import org.opentripplanner.routing.vehicle_parking.VehicleParking;

public class LegacyGraphQLBikeParkImpl implements LegacyGraphQLDataFetchers.LegacyGraphQLBikePark {
    @Override
    public DataFetcher<Relay.ResolvedGlobalId> id() {
        return environment -> new Relay.ResolvedGlobalId(
                "VehicleParking",
                getSource(environment).getId().toString()
        );
    }

    @Override
    public DataFetcher<String> bikeParkId() {
        return environment -> getSource(environment).getId().toString();
    }

    @Override
    public DataFetcher<String> name() {
        return environment -> getSource(environment).getName().toString();
    }

    @Override
    public DataFetcher<Integer> spacesAvailable() {
        return environment -> getSource(environment).getAvailability().getBicycleSpaces();
    }

    @Override
    public DataFetcher<Boolean> realtime() {
        return environment -> getSource(environment).hasRealTimeData();
    }

    @Override
    public DataFetcher<Double> lon() {
        return environment -> getSource(environment).getX();
    }

    @Override
    public DataFetcher<Double> lat() {
        return environment -> getSource(environment).getY();
    }

    private VehicleParking getSource(DataFetchingEnvironment environment) {
        return environment.getSource();
    }
}
