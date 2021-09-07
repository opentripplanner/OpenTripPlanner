package org.opentripplanner.ext.legacygraphqlapi.datafetchers;

import graphql.relay.Relay;
import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import org.opentripplanner.ext.legacygraphqlapi.generated.LegacyGraphQLDataFetchers;
import org.opentripplanner.routing.vehicle_rental.VehicleRentalStation;
import org.opentripplanner.routing.vehicle_rental.VehicleRentalStationUris;

import java.util.List;

public class LegacyGraphQLBikeRentalStationImpl implements LegacyGraphQLDataFetchers.LegacyGraphQLBikeRentalStation {
    @Override
    public DataFetcher<Relay.ResolvedGlobalId> id() {
        return environment -> new Relay.ResolvedGlobalId(
                "BikeRentalStation",
                getSource(environment).id.toString()
        );
    }

    @Override
    public DataFetcher<String> stationId() {
        return environment -> getSource(environment).getStationId();
    }

    @Override
    public DataFetcher<String> name() {
        return environment -> getSource(environment).name.toString(environment.getLocale());
    }

    @Override
    public DataFetcher<Integer> bikesAvailable() {
        return environment -> getSource(environment).vehiclesAvailable;
    }

    @Override
    public DataFetcher<Integer> spacesAvailable() {
        return environment -> getSource(environment).spacesAvailable;
    }

    //TODO:
    @Override
    public DataFetcher<String> state() {
        return environment -> null;
    }

    @Override
    public DataFetcher<Boolean> realtime() {
        return environment -> getSource(environment).realTimeData;
    }

    @Override
    public DataFetcher<Boolean> allowDropoff() {
        return environment -> getSource(environment).allowDropoff;
    }

    @Override
    public DataFetcher<Iterable<String>> networks() {
        return environment -> List.of(getSource(environment).getNetwork());
    }

    @Override
    public DataFetcher<Double> lon() {
        return environment -> getSource(environment).longitude;
    }

    @Override
    public DataFetcher<Double> lat() {
        return environment -> getSource(environment).latitude;
    }

    @Override
    public DataFetcher<Boolean> allowOverloading() {
        // TODO implement this
        return environment -> false;
    }

    @Override
    public DataFetcher<Integer> capacity() {
        // TODO implement this
        return environment -> 0;
    }

    @Override
    public DataFetcher<VehicleRentalStationUris> rentalUris() {
        return environment -> getSource(environment).rentalUris;
    }

    private VehicleRentalStation getSource(DataFetchingEnvironment environment) {
        return environment.getSource();
    }
}
