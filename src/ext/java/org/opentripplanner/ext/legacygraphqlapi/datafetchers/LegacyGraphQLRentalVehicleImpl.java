package org.opentripplanner.ext.legacygraphqlapi.datafetchers;

import graphql.relay.Relay;
import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import org.opentripplanner.ext.legacygraphqlapi.generated.LegacyGraphQLDataFetchers;
import org.opentripplanner.routing.vehicle_rental.VehicleRentalPlace;
import org.opentripplanner.routing.vehicle_rental.VehicleRentalStationUris;

import java.util.List;

public class LegacyGraphQLRentalVehicleImpl
        implements LegacyGraphQLDataFetchers.LegacyGraphQLRentalVehicle {

    @Override
    public DataFetcher<Relay.ResolvedGlobalId> id() {
        return environment -> new Relay.ResolvedGlobalId(
                "RentalVehicle",
                getSource(environment).getId().toString()
        );
    }

    @Override
    public DataFetcher<String> vehicleId() {
        return environment -> getSource(environment).getId().toString();
    }

    @Override
    public DataFetcher<String> name() {
        return environment -> getSource(environment).getName().toString(environment.getLocale());
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
    public DataFetcher<Boolean> operative() {
        return environment -> getSource(environment).isAllowPickup();
    }

    @Override
    public DataFetcher<VehicleRentalStationUris> rentalUris() {
        return environment -> getSource(environment).getRentalUris();
    }

    private VehicleRentalPlace getSource(DataFetchingEnvironment environment) {
        return environment.getSource();
    }
}
