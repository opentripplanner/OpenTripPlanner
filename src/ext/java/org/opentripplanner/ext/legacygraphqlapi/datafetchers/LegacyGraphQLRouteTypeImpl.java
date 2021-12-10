package org.opentripplanner.ext.legacygraphqlapi.datafetchers;

import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import org.opentripplanner.ext.legacygraphqlapi.generated.LegacyGraphQLDataFetchers;
import org.opentripplanner.ext.legacygraphqlapi.model.LegacyGraphQLRouteTypeModel;
import org.opentripplanner.model.Agency;

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

    private LegacyGraphQLRouteTypeModel getSource(DataFetchingEnvironment environment) {
        return environment.getSource();
    }
}
