package org.opentripplanner.apis.gtfs.datafetchers;

import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import java.time.format.DateTimeFormatter;
import org.opentripplanner.apis.gtfs.generated.GraphQLDataFetchers;
import org.opentripplanner.model.BookingTime;

public class BookingTimeImpl implements GraphQLDataFetchers.GraphQLBookingTime {

  @Override
  public DataFetcher<Integer> daysPrior() {
    return environment -> getSource(environment).getDaysPrior();
  }

  @Override
  public DataFetcher<String> time() {
    return environment -> getSource(environment).getTime().format(DateTimeFormatter.ISO_LOCAL_TIME);
  }

  private BookingTime getSource(DataFetchingEnvironment environment) {
    return environment.getSource();
  }
}
