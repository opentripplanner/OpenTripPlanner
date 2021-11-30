package org.opentripplanner.ext.legacygraphqlapi.datafetchers;

import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import org.opentripplanner.ext.legacygraphqlapi.LegacyGraphQLRequestContext;
import org.opentripplanner.ext.legacygraphqlapi.generated.LegacyGraphQLDataFetchers;
import org.opentripplanner.ext.legacygraphqlapi.model.LegacyGraphQLRouteType;
import org.opentripplanner.model.Agency;
import org.opentripplanner.routing.alertpatch.TransitAlert;

public class LegacyGraphQLRouteTypeImpl
        implements LegacyGraphQLDataFetchers.LegacyGraphQLRouteType {

    @Override
    public DataFetcher<Agency> agency() {
        return environment -> getSource(environment).getAgency();
    }

    @Override
    public DataFetcher<Iterable<TransitAlert>> alerts() {
        return environment -> {
            int routeType = getSource(environment).getRouteType();
            Agency agency = getSource(environment).getAgency();
            if (agency != null) {
                return environment.<LegacyGraphQLRequestContext>getContext()
                        .getRoutingService()
                        .getTransitAlertService()
                        .getRouteTypeAndAgencyAlerts(routeType, agency.getId());
            }
            return environment.<LegacyGraphQLRequestContext>getContext()
                    .getRoutingService()
                    .getTransitAlertService()
                    .getRouteTypeAlerts(routeType);
        };
    }

    @Override
    public DataFetcher<Integer> routeType() {
        return environment -> getSource(environment).getRouteType();
    }

    private LegacyGraphQLRouteType getSource(DataFetchingEnvironment environment) {
        return environment.getSource();
    }
}
