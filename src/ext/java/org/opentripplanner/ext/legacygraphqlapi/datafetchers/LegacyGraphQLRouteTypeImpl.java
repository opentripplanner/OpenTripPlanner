package org.opentripplanner.ext.legacygraphqlapi.datafetchers;

import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import java.util.stream.Collectors;
import org.opentripplanner.ext.legacygraphqlapi.LegacyGraphQLRequestContext;
import org.opentripplanner.ext.legacygraphqlapi.generated.LegacyGraphQLDataFetchers;
import org.opentripplanner.ext.legacygraphqlapi.model.LegacyGraphQLRouteTypeModel;
import org.opentripplanner.model.Agency;
import org.opentripplanner.model.Route;
import org.opentripplanner.routing.RoutingService;

public class LegacyGraphQLRouteTypeImpl
        implements LegacyGraphQLDataFetchers.LegacyGraphQLRouteType {

    @Override
    public DataFetcher<Agency> agency() {
        return environment -> getSource(environment).getAgency();
    }

    @Override
    public DataFetcher<Integer> routeType() {
        return environment -> getSource(environment).getRouteType();
    }

    @Override
    public DataFetcher<Iterable<Route>> routes() {
        return environment -> {
            Agency agency = getSource(environment).getAgency();
            return getRoutingService(environment).getAllRoutes()
                    .stream()
                    .filter(route ->
                            route.getId().getFeedId().equals(getSource(environment).getFeedId())
                                    && route.getGtfsType() == getSource(environment).getRouteType() && (
                                    agency == null || route.getAgency()
                                            .equals(agency)
                            ))
                    .collect(
                            Collectors.toList());
        };
    }

    private RoutingService getRoutingService(DataFetchingEnvironment environment) {
        return environment.<LegacyGraphQLRequestContext>getContext().getRoutingService();
    }

    private LegacyGraphQLRouteTypeModel getSource(DataFetchingEnvironment environment) {
        return environment.getSource();
    }
}
