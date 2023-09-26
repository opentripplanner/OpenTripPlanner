package org.opentripplanner.apis.gtfs.datafetchers;

import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import org.opentripplanner.apis.gtfs.generated.GraphQLDataFetchers;
import org.opentripplanner.model.BookingInfo;
import org.opentripplanner.model.BookingTime;
import org.opentripplanner.transit.model.organization.ContactInfo;

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
  public DataFetcher<Long> maximumBookingNoticeSeconds() {
    return environment -> getSource(environment).getMaximumBookingNotice().toSeconds();
  }

  @Override
  public DataFetcher<String> message() {
    return environment -> getSource(environment).getMessage();
  }

  @Override
  public DataFetcher<Long> minimumBookingNoticeSeconds() {
    return environment -> getSource(environment).getMinimumBookingNotice().toSeconds();
  }

  @Override
  public DataFetcher<String> pickupMessage() {
    return environment -> getSource(environment).getPickupMessage();
  }

  private BookingInfo getSource(DataFetchingEnvironment environment) {
    return environment.getSource();
  }
}
