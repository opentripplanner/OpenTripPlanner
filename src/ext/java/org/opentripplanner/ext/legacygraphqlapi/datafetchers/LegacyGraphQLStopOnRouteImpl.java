package org.opentripplanner.ext.legacygraphqlapi.datafetchers;

import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import org.opentripplanner.ext.legacygraphqlapi.generated.LegacyGraphQLDataFetchers;
import org.opentripplanner.ext.legacygraphqlapi.model.LegacyGraphQLStopOnRouteModel;
import org.opentripplanner.model.Route;

public class LegacyGraphQLStopOnRouteImpl
        implements LegacyGraphQLDataFetchers.LegacyGraphQLStopOnRoute {

    @Override
    public DataFetcher<Object> stop() {
        return environment -> getSource(environment).getStop();
    }

    @Override
    public DataFetcher<Route> route() {
        return environment -> getSource(environment).getRoute();
    }

    private LegacyGraphQLStopOnRouteModel getSource(DataFetchingEnvironment environment) {
        return environment.getSource();
    }
}
