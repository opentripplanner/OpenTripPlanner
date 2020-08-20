package org.opentripplanner.ext.legacygraphqlapi.datafetchers;

import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import org.opentripplanner.ext.legacygraphqlapi.LegacyGraphQLRequestContext;
import org.opentripplanner.ext.legacygraphqlapi.generated.LegacyGraphQLDataFetchers;
import org.opentripplanner.routing.RoutingService;

public class LegacyGraphQLserviceTimeRangeImpl implements LegacyGraphQLDataFetchers.LegacyGraphQLServiceTimeRange {
    @Override
    public DataFetcher<Long> start() {
        return environment -> getRoutingService(environment).getTransitServiceStarts();
    }

    @Override
    public DataFetcher<Long> end() {
        return environment -> getRoutingService(environment).getTransitServiceEnds();
    }

    private RoutingService getRoutingService(DataFetchingEnvironment environment) {
        return environment.<LegacyGraphQLRequestContext>getContext().getRoutingService();
    }
}
