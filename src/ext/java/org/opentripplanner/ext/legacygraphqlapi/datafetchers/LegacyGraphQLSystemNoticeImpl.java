package org.opentripplanner.ext.legacygraphqlapi.datafetchers;

import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import org.opentripplanner.ext.legacygraphqlapi.generated.LegacyGraphQLDataFetchers;
import org.opentripplanner.model.SystemNotice;

public class LegacyGraphQLSystemNoticeImpl
        implements LegacyGraphQLDataFetchers.LegacyGraphQLSystemNotice {

    @Override
    public DataFetcher<String> tag() {
        return environment -> getSource(environment).tag;
    }

    @Override
    public DataFetcher<String> text() {
        return environment -> getSource(environment).text;
    }

    private SystemNotice getSource(DataFetchingEnvironment environment) {
        return environment.getSource();
    }
}
