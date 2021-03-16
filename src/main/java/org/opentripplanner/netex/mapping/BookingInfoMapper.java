package org.opentripplanner.netex.mapping;

import com.esotericsoftware.minlog.Log;
import org.opentripplanner.model.BookingInfo;
import org.opentripplanner.model.BookingMethod;
import org.opentripplanner.model.ContactInfo;
import org.rutebanken.netex.model.BookingArrangementsStructure;
import org.rutebanken.netex.model.BookingMethodEnumeration;
import org.rutebanken.netex.model.ContactStructure;
import org.rutebanken.netex.model.FlexibleLine;
import org.rutebanken.netex.model.FlexibleServiceProperties;
import org.rutebanken.netex.model.MultilingualString;
import org.rutebanken.netex.model.PurchaseWhenEnumeration;
import org.rutebanken.netex.model.ServiceJourney;
import org.rutebanken.netex.model.StopPointInJourneyPattern;

import java.time.Duration;
import java.time.LocalTime;
import java.util.EnumSet;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Maps booking info from NeTEx BookingArrangements, FlexibleServiceProperties, and FlexibleLine
 * into OTP BookingInfo.
 *
 * The precedence is as follows:
 * 1. BookingArrangements
 * 2. FlexibleServiceProperties
 * 3. FlexibleLine
 */
public class BookingInfoMapper {

  static BookingInfo map(
      StopPointInJourneyPattern stopPoint,
      ServiceJourney serviceJourney,
      FlexibleLine flexibleLine
  ) {
    BookingInfo bookingInfo = null;

    if (stopPoint.getBookingArrangements() != null) {
      bookingInfo = map(stopPoint.getBookingArrangements());
    } else if (serviceJourney != null
        && serviceJourney.getFlexibleServiceProperties() != null) {
      bookingInfo = map(serviceJourney.getFlexibleServiceProperties());
    } else if (flexibleLine != null) {
      bookingInfo = map(flexibleLine);
    }

    return bookingInfo;
  }

  private static BookingInfo map(BookingArrangementsStructure bookingArrangements) {
    return map(
        bookingArrangements.getBookingContact(),
        bookingArrangements.getBookingMethods(),
        bookingArrangements.getLatestBookingTime(),
        bookingArrangements.getBookWhen(),
        bookingArrangements.getMinimumBookingPeriod(),
        bookingArrangements.getBookingNote()
    );
  }

  private static BookingInfo map(FlexibleServiceProperties serviceProperties) {
    return map(
        serviceProperties.getBookingContact(),
        serviceProperties.getBookingMethods(),
        serviceProperties.getLatestBookingTime(),
        serviceProperties.getBookWhen(),
        serviceProperties.getMinimumBookingPeriod(),
        serviceProperties.getBookingNote()
    );
  }

  private static BookingInfo map(FlexibleLine flexibleLine) {
    return map(
        flexibleLine.getBookingContact(),
        flexibleLine.getBookingMethods(),
        flexibleLine.getLatestBookingTime(),
        flexibleLine.getBookWhen(),
        flexibleLine.getMinimumBookingPeriod(),
        flexibleLine.getBookingNote()
    );
  }

  private static BookingInfo map(
      ContactStructure contactStructure,
      List<BookingMethodEnumeration> bookingMethodEnum,
      LocalTime latestBookingTime,
      PurchaseWhenEnumeration bookWhen,
      Duration minimumBookingPeriod,
      MultilingualString bookingNote
  ) {

    if (contactStructure == null) { return null; }

    ContactInfo contactInfo = new ContactInfo(
        contactStructure.getContactPerson() != null
            ? contactStructure.getContactPerson().getValue()
            : null,
        contactStructure.getPhone(),
        contactStructure.getEmail(),
        contactStructure.getFax(),
        null,
        contactStructure.getUrl(),
        contactStructure.getFurtherDetails() != null
            ? contactStructure.getFurtherDetails()
            .getValue()
            : null
    );

    EnumSet<BookingMethod> bookingMethods = bookingMethodEnum
        .stream()
        .map(BookingMethodMapper::map)
        .collect(Collectors.toCollection(() -> EnumSet.noneOf(BookingMethod.class)));

    BookingTime otpEarliestBookingTime = null;
    BookingTime otpLatestBookingTime = null;
    Duration minimumBookingNotice = null;

    if (latestBookingTime != null && bookWhen != null) {
      otpEarliestBookingTime = mapEarliestBookingTime(bookWhen);
      otpLatestBookingTime = mapLatestBookingTime(latestBookingTime, bookWhen);

      if (minimumBookingPeriod != null)
        Log.warn("MinimumBookingPeriod cannot be set if latestBookingTime is set. "
            + "MinimumBookingPeriod will be ignored for: " + contactStructure);
    }
    else if (minimumBookingPeriod != null) {
      minimumBookingNotice = minimumBookingPeriod;
    }

    BookingInfo bookingInfo = new BookingInfo(contactInfo,
        bookingMethods,
        otpEarliestBookingTime,
        otpLatestBookingTime,
        minimumBookingNotice,
        Duration.ZERO,
        bookingNote != null ? bookingNote.getValue() : null,
        null,
        null
    );

    return bookingInfo;
  }

  private static BookingTime mapLatestBookingTime(LocalTime latestBookingTime, PurchaseWhenEnumeration purchaseWhen) {
    switch (purchaseWhen) {
      case UNTIL_PREVIOUS_DAY:
        return new BookingTime(latestBookingTime, 1);
      case DAY_OF_TRAVEL_ONLY:
      case ADVANCE_ONLY:
        return new BookingTime(latestBookingTime, 0);
      default:
        throw new IllegalArgumentException("Value not supported: " + purchaseWhen.toString());
    }
  }

  private static BookingTime mapEarliestBookingTime(PurchaseWhenEnumeration purchaseWhen) {
    switch (purchaseWhen) {
      case UNTIL_PREVIOUS_DAY:
      case ADVANCE_ONLY:
        return null;
      case DAY_OF_TRAVEL_ONLY:
        return new BookingTime(LocalTime.MIDNIGHT, 0);
      default:
        throw new IllegalArgumentException("Value not supported: " + purchaseWhen.toString());
    }
  }
}
