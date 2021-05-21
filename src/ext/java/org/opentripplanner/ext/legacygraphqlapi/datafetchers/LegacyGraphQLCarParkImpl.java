package org.opentripplanner.ext.legacygraphqlapi.datafetchers;

import graphql.relay.Relay;
import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import org.opentripplanner.ext.legacygraphqlapi.generated.LegacyGraphQLDataFetchers;
import org.opentripplanner.routing.vehicle_parking.VehicleParking;

public class LegacyGraphQLCarParkImpl implements LegacyGraphQLDataFetchers.LegacyGraphQLCarPark {

    @Override
    public DataFetcher<Relay.ResolvedGlobalId> id() {
        return environment -> new Relay.ResolvedGlobalId(
                "CarPark",
                getSource(environment).getId().toString()
        );
    }

    @Override
    public DataFetcher<String> carParkId() {
        return environment -> getSource(environment).getId().toString();
    }

    @Override
    public DataFetcher<String> name() {
        return environment -> getSource(environment).getName().toString();
    }

    @Override
    public DataFetcher<Integer> maxCapacity() {
        return environment -> {
            var availability = getSource(environment).getCapacity();
            if (availability == null) {
                return null;
            }
            return availability.getCarSpaces();
        };
    }

    @Override
    public DataFetcher<Integer> spacesAvailable() {
        return environment -> {
            var availability = getSource(environment).getAvailability();
            if (availability == null) {
                return null;
            }
            return availability.getCarSpaces();
        };
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
