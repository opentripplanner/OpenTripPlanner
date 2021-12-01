package org.opentripplanner.ext.legacygraphqlapi.datafetchers;

import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import java.util.Collection;
import org.opentripplanner.ext.legacygraphqlapi.LegacyGraphQLRequestContext;
import org.opentripplanner.ext.legacygraphqlapi.generated.LegacyGraphQLDataFetchers;
import org.opentripplanner.ext.legacygraphqlapi.model.LegacyGraphQLRouteTypeModel;
import org.opentripplanner.model.Agency;
import org.opentripplanner.routing.alertpatch.TransitAlert;
import org.opentripplanner.routing.services.TransitAlertService;

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
            String feedId = agency.getId().getFeedId();
            TransitAlertService service = environment.<LegacyGraphQLRequestContext>getContext()
                    .getRoutingService()
                    .getTransitAlertService();
            if (agency != null) {
                Collection<TransitAlert> routeTypeAndAgencyAlerts =
                        service.getRouteTypeAndAgencyAlerts(routeType, agency.getId());
                routeTypeAndAgencyAlerts.addAll(service.getRouteTypeAlerts(routeType, feedId));
                return routeTypeAndAgencyAlerts;
            }
            return service.getRouteTypeAlerts(routeType, feedId);
        };
    }

    @Override
    public DataFetcher<Integer> routeType() {
        return environment -> getSource(environment).getRouteType();
    }

    private LegacyGraphQLRouteTypeModel getSource(DataFetchingEnvironment environment) {
        return environment.getSource();
    }
}
