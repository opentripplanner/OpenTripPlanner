package org.opentripplanner.ext.legacygraphqlapi.datafetchers;

import graphql.relay.Relay;
import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import org.opentripplanner.ext.legacygraphqlapi.generated.LegacyGraphQLDataFetchers;
import org.opentripplanner.routing.vehicle_parking.VehicleParking;

public class LegacyGraphQLVehicleParkingImpl implements LegacyGraphQLDataFetchers.LegacyGraphQLVehicleParking {
    @Override
    public DataFetcher<Relay.ResolvedGlobalId> id() {
        return environment -> new Relay.ResolvedGlobalId("VehicleParking",
            getSource(environment).getId().toString());
    }

    @Override
    public DataFetcher<String> vehicleParkingId() {
        return environment -> getSource(environment).getId().toString();
    }

    @Override
    public DataFetcher<String> name() {
        return environment -> getSource(environment).getName().toString();
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

    @Override
    public DataFetcher<String> detailsUrl() {
        return environment -> getSource(environment).getDetailsUrl();
    }

    @Override
    public DataFetcher<String> imageUrl() {
        return environment -> getSource(environment).getImageUrl();
    }

    @Override
    public DataFetcher<Iterable<String>> tags() {
        return environment -> getSource(environment).getTags();
    }

    @Override
    public DataFetcher<String> note() {
        return environment -> {
            var note = getSource(environment).getNote();
            return note != null ? note.toString() : null;
        };
    }

    @Override
    public DataFetcher<VehicleParking.VehicleParkingState> state() {
        return environment -> getSource(environment).getState();
    }

    @Override
    public DataFetcher<Boolean> bicyclePlaces() {
        return environment -> getSource(environment).hasBicyclePlaces();
    }

    @Override
    public DataFetcher<Boolean> carPlaces() {
        return environment -> getSource(environment).hasCarPlaces();
    }

    @Override
    public DataFetcher<Boolean> wheelchairAccessibleCarPlaces() {
        return environment -> getSource(environment).hasWheelchairAccessibleCarPlaces();
    }

    @Override
    public DataFetcher<VehicleParking.VehiclePlaces> capacity() {
        return environment -> getSource(environment).getCapacity();
    }

    @Override
    public DataFetcher<VehicleParking.VehiclePlaces> availability() {
        return environment -> getSource(environment).getAvailability();
    }

    private VehicleParking getSource(DataFetchingEnvironment environment) {
        return environment.getSource();
    }
}
