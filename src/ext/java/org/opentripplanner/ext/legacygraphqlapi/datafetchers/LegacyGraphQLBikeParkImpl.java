package org.opentripplanner.ext.legacygraphqlapi.datafetchers;

import graphql.relay.Relay;
import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import org.opentripplanner.ext.legacygraphqlapi.generated.LegacyGraphQLDataFetchers;
import org.opentripplanner.routing.bike_park.BikePark;

public class LegacyGraphQLBikeParkImpl implements LegacyGraphQLDataFetchers.LegacyGraphQLBikePark {
    @Override
    public DataFetcher<Relay.ResolvedGlobalId> id() {
        return environment -> new Relay.ResolvedGlobalId(
                "BikePark",
                getSource(environment).id
        );
    }

    @Override
    public DataFetcher<String> bikeParkId() {
        return environment -> getSource(environment).id;
    }

    @Override
    public DataFetcher<String> name() {
        return environment -> getSource(environment).name;
    }

    @Override
    public DataFetcher<Integer> spacesAvailable() {
        return environment -> getSource(environment).spacesAvailable;
    }

    @Override
    public DataFetcher<Boolean> realtime() {
        return environment -> getSource(environment).realTimeData;
    }

    @Override
    public DataFetcher<Double> lon() {
        return environment -> getSource(environment).x;
    }

    @Override
    public DataFetcher<Double> lat() {
        return environment -> getSource(environment).y;
    }

    private BikePark getSource(DataFetchingEnvironment environment) {
        return environment.getSource();
    }
}
