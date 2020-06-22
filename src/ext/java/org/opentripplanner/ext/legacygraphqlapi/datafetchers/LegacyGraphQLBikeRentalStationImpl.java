package org.opentripplanner.ext.legacygraphqlapi.datafetchers;

import graphql.relay.Relay;
import graphql.schema.DataFetcher;
import org.opentripplanner.ext.legacygraphqlapi.generated.LegacyGraphQLDataFetchers;

public class LegacyGraphQLBikeRentalStationImpl implements LegacyGraphQLDataFetchers.LegacyGraphQLBikeRentalStation {
    @Override
    public DataFetcher<Relay.ResolvedGlobalId> id() {
        return null;
    }

    @Override
    public DataFetcher<String> stationId() {
        return null;
    }

    @Override
    public DataFetcher<String> name() {
        return null;
    }

    @Override
    public DataFetcher<Integer> bikesAvailable() {
        return null;
    }

    @Override
    public DataFetcher<Integer> spacesAvailable() {
        return null;
    }

    @Override
    public DataFetcher<String> state() {
        return null;
    }

    @Override
    public DataFetcher<Boolean> realtime() {
        return null;
    }

    @Override
    public DataFetcher<Boolean> allowDropoff() {
        return null;
    }

    @Override
    public DataFetcher<Iterable<String>> networks() {
        return null;
    }

    @Override
    public DataFetcher<Double> lon() {
        return null;
    }

    @Override
    public DataFetcher<Double> lat() {
        return null;
    }
}
