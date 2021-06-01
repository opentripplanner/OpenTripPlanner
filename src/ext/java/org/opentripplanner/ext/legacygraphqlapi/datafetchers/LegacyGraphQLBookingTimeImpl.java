package org.opentripplanner.ext.legacygraphqlapi.datafetchers;

import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import java.time.format.DateTimeFormatter;
import org.opentripplanner.ext.legacygraphqlapi.generated.LegacyGraphQLDataFetchers;
import org.opentripplanner.model.BookingTime;

public class LegacyGraphQLBookingTimeImpl implements LegacyGraphQLDataFetchers.LegacyGraphQLBookingTime {
    @Override
    public DataFetcher<String> time() {
        return environment -> getSource(environment).getTime().format(DateTimeFormatter.ISO_LOCAL_TIME);
    }

    @Override
    public DataFetcher<Integer> daysPrior() {
        return environment -> getSource(environment).getDaysPrior();
    }

    private BookingTime getSource(DataFetchingEnvironment environment) {
        return environment.getSource();
    }
}
