package org.opentripplanner.apis.gtfs.datafetchers;

import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import java.time.Duration;
import org.opentripplanner.apis.gtfs.generated.GraphQLDataFetchers;
import org.opentripplanner.transit.model.organization.ContactInfo;
import org.opentripplanner.transit.model.timetable.booking.BookingInfo;
import org.opentripplanner.transit.model.timetable.booking.BookingTime;

public class BookingInfoImpl implements GraphQLDataFetchers.GraphQLBookingInfo {

  @Override
  public DataFetcher<ContactInfo> contactInfo() {
    return environment -> getSource(environment).getContactInfo();
  }

  @Override
  public DataFetcher<String> dropOffMessage() {
    return environment -> getSource(environment).getDropOffMessage();
  }

  @Override
  public DataFetcher<BookingTime> earliestBookingTime() {
    return environment -> getSource(environment).getEarliestBookingTime();
  }

  @Override
  public DataFetcher<BookingTime> latestBookingTime() {
    return environment -> getSource(environment).getLatestBookingTime();
  }

  @Override
  public DataFetcher<Duration> maximumBookingNotice() {
    return env -> getSource(env).getMaximumBookingNotice().orElse(null);
  }

  @Override
  public DataFetcher<Long> maximumBookingNoticeSeconds() {
    return environment ->
      getSource(environment).getMaximumBookingNotice().map(Duration::toSeconds).orElse(null);
  }

  @Override
  public DataFetcher<String> message() {
    return environment -> getSource(environment).getMessage();
  }

  @Override
  public DataFetcher<Duration> minimumBookingNotice() {
    return env -> getSource(env).getMinimumBookingNotice().orElse(null);
  }

  @Override
  public DataFetcher<Long> minimumBookingNoticeSeconds() {
    return environment ->
      getSource(environment).getMinimumBookingNotice().map(Duration::toSeconds).orElse(null);
  }

  @Override
  public DataFetcher<String> pickupMessage() {
    return environment -> getSource(environment).getPickupMessage();
  }

  private BookingInfo getSource(DataFetchingEnvironment environment) {
    return environment.getSource();
  }
}
