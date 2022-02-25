package org.opentripplanner.ext.legacygraphqlapi.datafetchers;

import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import org.opentripplanner.ext.legacygraphqlapi.generated.LegacyGraphQLDataFetchers;
import org.opentripplanner.ext.legacygraphqlapi.model.LegacyGraphQLUnknownModel;

public class LegacyGraphQLUnknownImpl
        implements LegacyGraphQLDataFetchers.LegacyGraphQLUnknown {

    @Override
    public DataFetcher<String> description() {
        return environment -> getSource(environment).getDescription();
    }

    private LegacyGraphQLUnknownModel getSource(DataFetchingEnvironment environment) {
        return environment.getSource();
    }
}
