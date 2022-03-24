package org.opentripplanner.ext.legacygraphqlapi.datafetchers;

import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import org.opentripplanner.ext.legacygraphqlapi.generated.LegacyGraphQLDataFetchers;
import org.opentripplanner.ext.legacygraphqlapi.model.LegacyGraphQLStopOnTripModel;
import org.opentripplanner.model.Trip;

public class LegacyGraphQLStopOnTripImpl
        implements LegacyGraphQLDataFetchers.LegacyGraphQLStopOnTrip {

    @Override
    public DataFetcher<Object> stop() {
        return environment -> getSource(environment).getStop();
    }

    @Override
    public DataFetcher<Trip> trip() {
        return environment -> getSource(environment).getTrip();
    }

    private LegacyGraphQLStopOnTripModel getSource(DataFetchingEnvironment environment) {
        return environment.getSource();
    }
}
