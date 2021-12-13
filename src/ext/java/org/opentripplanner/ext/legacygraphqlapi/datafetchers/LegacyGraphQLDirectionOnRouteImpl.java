package org.opentripplanner.ext.legacygraphqlapi.datafetchers;

import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import org.opentripplanner.ext.legacygraphqlapi.generated.LegacyGraphQLDataFetchers;
import org.opentripplanner.ext.legacygraphqlapi.model.LegacyGraphQLDirectionOnRouteModel;
import org.opentripplanner.model.Route;

public class LegacyGraphQLDirectionOnRouteImpl
        implements LegacyGraphQLDataFetchers.LegacyGraphQLDirectionOnRoute {

    @Override
    public DataFetcher<Integer> directionId() {
        return environment -> getSource(environment).getDirectionId();
    }

    @Override
    public DataFetcher<Route> route() {
        return environment -> getSource(environment).getRoute();
    }

    private LegacyGraphQLDirectionOnRouteModel getSource(DataFetchingEnvironment environment) {
        return environment.getSource();
    }
}
