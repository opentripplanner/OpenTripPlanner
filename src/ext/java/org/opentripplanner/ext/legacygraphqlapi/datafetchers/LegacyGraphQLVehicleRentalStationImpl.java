package org.opentripplanner.ext.legacygraphqlapi.datafetchers;

import graphql.relay.Relay;
import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import org.opentripplanner.ext.legacygraphqlapi.generated.LegacyGraphQLDataFetchers;
import org.opentripplanner.routing.vehicle_rental.VehicleRentalPlace;
import org.opentripplanner.routing.vehicle_rental.VehicleRentalStationUris;

import java.util.List;

public class LegacyGraphQLVehicleRentalStationImpl
        implements LegacyGraphQLDataFetchers.LegacyGraphQLVehicleRentalStation {

    @Override
    public DataFetcher<Relay.ResolvedGlobalId> id() {
        return environment -> new Relay.ResolvedGlobalId(
                "VehicleRentalStation",
                getSource(environment).getId().toString()
        );
    }

    @Override
    public DataFetcher<String> stationId() {
        return environment -> getSource(environment).getId().toString();
    }

    @Override
    public DataFetcher<String> name() {
        return environment -> getSource(environment).getName().toString(environment.getLocale());
    }

    @Override
    public DataFetcher<Integer> vehiclesAvailable() {
        return environment -> getSource(environment).getVehiclesAvailable();
    }

    @Override
    public DataFetcher<Integer> spacesAvailable() {
        return environment -> getSource(environment).getSpacesAvailable();
    }

    @Override
    public DataFetcher<Boolean> realtime() {
        return environment -> getSource(environment).isRealTimeData();
    }

    @Override
    public DataFetcher<Boolean> allowDropoff() {
        return environment -> getSource(environment).isAllowDropoff();
    }

    @Override
    public DataFetcher<Boolean> allowDropoffNow() {
        return environment -> getSource(environment).allowDropoffNow();
    }

    @Override
    public DataFetcher<Boolean> allowPickup() {
        return environment -> getSource(environment).isAllowPickup();
    }

    @Override
    public DataFetcher<Boolean> allowPickupNow() {
        return environment -> getSource(environment).allowPickupNow();
    }

    @Override
    public DataFetcher<String> network() {
        return environment -> getSource(environment).getNetwork();
    }

    @Override
    public DataFetcher<Double> lon() {
        return environment -> getSource(environment).getLongitude();
    }

    @Override
    public DataFetcher<Double> lat() {
        return environment -> getSource(environment).getLatitude();
    }

    @Override
    public DataFetcher<Boolean> allowOverloading() {
        return environment -> getSource(environment).isAllowOverloading();
    }

    @Override
    public DataFetcher<Integer> capacity() {
        return environment -> getSource(environment).getCapacity();
    }

    @Override
    public DataFetcher<Boolean> operative() {
        return environment -> getSource(environment).isAllowPickup() && getSource(
                environment).isAllowDropoff();
    }

    @Override
    public DataFetcher<VehicleRentalStationUris> rentalUris() {
        return environment -> getSource(environment).getRentalUris();
    }

    private VehicleRentalPlace getSource(DataFetchingEnvironment environment) {
        return environment.getSource();
    }
}
