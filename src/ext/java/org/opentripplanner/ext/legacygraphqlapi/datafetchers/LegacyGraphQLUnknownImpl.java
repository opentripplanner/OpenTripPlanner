package org.opentripplanner.ext.legacygraphqlapi.datafetchers;

import graphql.schema.DataFetcher;
import org.opentripplanner.ext.legacygraphqlapi.generated.LegacyGraphQLDataFetchers;

public class LegacyGraphQLUnknownImpl
        implements LegacyGraphQLDataFetchers.LegacyGraphQLUnknown {

    @Override
    public DataFetcher<String> description() {
        return environment -> null;
    }
}
