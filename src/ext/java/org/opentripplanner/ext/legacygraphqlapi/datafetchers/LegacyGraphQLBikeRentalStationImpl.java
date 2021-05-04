package org.opentripplanner.ext.legacygraphqlapi.datafetchers;

import graphql.relay.Relay;
import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import org.opentripplanner.ext.legacygraphqlapi.generated.LegacyGraphQLDataFetchers;
import org.opentripplanner.routing.bike_rental.BikeRentalStation;

public class LegacyGraphQLBikeRentalStationImpl implements LegacyGraphQLDataFetchers.LegacyGraphQLBikeRentalStation {
    @Override
    public DataFetcher<Relay.ResolvedGlobalId> id() {
        return environment -> new Relay.ResolvedGlobalId(
                "BikeRentalStation",
                getSource(environment).id
        );
    }

    @Override
    public DataFetcher<String> stationId() {
        return environment -> getSource(environment).id;
    }

    @Override
    public DataFetcher<String> name() {
        return environment -> getSource(environment).getName();
    }

    @Override
    public DataFetcher<Integer> bikesAvailable() {
        return environment -> getSource(environment).bikesAvailable;
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
        return environment -> getSource(environment).networks;
    }

    @Override
    public DataFetcher<Double> lon() {
        return environment -> getSource(environment).x;
    }

    @Override
    public DataFetcher<Double> lat() {
        return environment -> getSource(environment).y;
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

    private BikeRentalStation getSource(DataFetchingEnvironment environment) {
        return environment.getSource();
    }
}
