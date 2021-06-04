package org.opentripplanner.ext.legacygraphqlapi.datafetchers;

import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import org.opentripplanner.ext.legacygraphqlapi.generated.LegacyGraphQLDataFetchers;
import org.opentripplanner.model.BookingInfo;
import org.opentripplanner.model.BookingTime;
import org.opentripplanner.model.ContactInfo;

public class LegacyGraphQLBookingInfoImpl implements LegacyGraphQLDataFetchers.LegacyGraphQLBookingInfo {
    @Override
    public DataFetcher<ContactInfo> contactInfo() {
        return environment -> getSource(environment).getContactInfo();
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
    public DataFetcher<Long> minimumBookingNoticeSeconds() {
        return environment -> getSource(environment).getMinimumBookingNotice().toSeconds();
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
    public DataFetcher<String> pickupMessage() {
        return environment -> getSource(environment).getPickupMessage();
    }

    @Override
    public DataFetcher<String> dropOffMessage() {
        return environment -> getSource(environment).getDropOffMessage();
    }

    private BookingInfo getSource(DataFetchingEnvironment environment) {
        return environment.getSource();
    }
}
