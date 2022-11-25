package org.opentripplanner.netex.mapping;

import java.time.Duration;
import java.time.LocalTime;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import org.opentripplanner.graph_builder.issue.api.DataImportIssueStore;
import org.opentripplanner.model.BookingInfo;
import org.opentripplanner.model.BookingMethod;
import org.opentripplanner.model.BookingTime;
import org.opentripplanner.transit.model.organization.ContactInfo;
import org.rutebanken.netex.model.BookingArrangementsStructure;
import org.rutebanken.netex.model.BookingMethodEnumeration;
import org.rutebanken.netex.model.ContactStructure;
import org.rutebanken.netex.model.EntityStructure;
import org.rutebanken.netex.model.FlexibleLine;
import org.rutebanken.netex.model.FlexibleServiceProperties;
import org.rutebanken.netex.model.MultilingualString;
import org.rutebanken.netex.model.PurchaseWhenEnumeration;
import org.rutebanken.netex.model.ServiceJourney;
import org.rutebanken.netex.model.StopPointInJourneyPattern;

/**
 * Maps booking info from NeTEx BookingArrangements, FlexibleServiceProperties, and FlexibleLine
 * into OTP BookingInfo.
 * <p>
 * The precedence is as follows: 1. BookingArrangements 2. FlexibleServiceProperties 3.
 * FlexibleLine
 */
public class BookingInfoMapper {

  private final DataImportIssueStore issueStore;

  BookingInfoMapper(DataImportIssueStore issueStore) {
    this.issueStore = issueStore;
  }

  @Nullable
  BookingInfo map(
    StopPointInJourneyPattern stopPoint,
    ServiceJourney serviceJourney,
    FlexibleLine flexibleLine
  ) {
    if (stopPoint.getBookingArrangements() != null) {
      return map(stopPoint.getBookingArrangements(), ref("StopPoint", stopPoint));
    } else if (serviceJourney != null && serviceJourney.getFlexibleServiceProperties() != null) {
      return map(
        serviceJourney.getFlexibleServiceProperties(),
        ref("ServiceJourney", serviceJourney)
      );
    } else if (flexibleLine != null) {
      return map(flexibleLine, ref("FlexibleLine", flexibleLine));
    }
    return null;
  }

  private static BookingTime mapLatestBookingTime(
    LocalTime latestBookingTime,
    PurchaseWhenEnumeration purchaseWhen
  ) {
    switch (purchaseWhen) {
      case UNTIL_PREVIOUS_DAY:
        return new BookingTime(latestBookingTime, 1);
      case DAY_OF_TRAVEL_ONLY:
      case ADVANCE_ONLY:
      case ADVANCE_AND_DAY_OF_TRAVEL:
        return new BookingTime(latestBookingTime, 0);
      case TIME_OF_TRAVEL_ONLY:
        return null;
      default:
        throw new IllegalArgumentException("Value not supported: " + purchaseWhen);
    }
  }

  private static BookingTime mapEarliestBookingTime(PurchaseWhenEnumeration purchaseWhen) {
    switch (purchaseWhen) {
      case UNTIL_PREVIOUS_DAY:
      case ADVANCE_ONLY:
      case ADVANCE_AND_DAY_OF_TRAVEL:
      case TIME_OF_TRAVEL_ONLY:
        return null;
      case DAY_OF_TRAVEL_ONLY:
        return new BookingTime(LocalTime.MIDNIGHT, 0);
      default:
        throw new IllegalArgumentException("Value not supported: " + purchaseWhen);
    }
  }

  private static String ref(String type, EntityStructure entity) {
    return type + "(" + entity.getId() + ")";
  }

  private BookingInfo map(BookingArrangementsStructure bookingArrangements, String entityRef) {
    return map(
      bookingArrangements.getBookingContact(),
      bookingArrangements.getBookingMethods(),
      bookingArrangements.getLatestBookingTime(),
      bookingArrangements.getBookWhen(),
      bookingArrangements.getMinimumBookingPeriod(),
      bookingArrangements.getBookingNote(),
      entityRef
    );
  }

  private BookingInfo map(FlexibleServiceProperties serviceProperties, String entityRef) {
    return map(
      serviceProperties.getBookingContact(),
      serviceProperties.getBookingMethods(),
      serviceProperties.getLatestBookingTime(),
      serviceProperties.getBookWhen(),
      serviceProperties.getMinimumBookingPeriod(),
      serviceProperties.getBookingNote(),
      entityRef
    );
  }

  private BookingInfo map(FlexibleLine flexibleLine, String entityRef) {
    return map(
      flexibleLine.getBookingContact(),
      flexibleLine.getBookingMethods(),
      flexibleLine.getLatestBookingTime(),
      flexibleLine.getBookWhen(),
      flexibleLine.getMinimumBookingPeriod(),
      flexibleLine.getBookingNote(),
      entityRef
    );
  }

  private BookingInfo map(
    ContactStructure contactStructure,
    List<BookingMethodEnumeration> bookingMethodEnum,
    LocalTime latestBookingTime,
    PurchaseWhenEnumeration bookWhen,
    Duration minimumBookingPeriod,
    MultilingualString bookingNote,
    String entityRef
  ) {
    if (contactStructure == null) {
      return null;
    }

    ContactInfo contactInfo = ContactInfo
      .of()
      .withContactPerson(
        contactStructure.getContactPerson() != null
          ? contactStructure.getContactPerson().getValue()
          : null
      )
      .withPhoneNumber(contactStructure.getPhone())
      .withEMail(contactStructure.getEmail())
      .withFaxNumber(contactStructure.getFax())
      .withBookingUrl(contactStructure.getUrl())
      .withAdditionalDetails(
        contactStructure.getFurtherDetails() != null
          ? contactStructure.getFurtherDetails().getValue()
          : null
      )
      .build();

    EnumSet<BookingMethod> bookingMethods = bookingMethodEnum
      .stream()
      .map(bm -> BookingMethodMapper.map(entityRef, bm))
      .filter(Objects::nonNull)
      .collect(Collectors.toCollection(() -> EnumSet.noneOf(BookingMethod.class)));

    BookingTime otpEarliestBookingTime = null;
    BookingTime otpLatestBookingTime = null;
    Duration minimumBookingNotice = null;

    if (latestBookingTime != null && bookWhen != null) {
      otpEarliestBookingTime = mapEarliestBookingTime(bookWhen);
      otpLatestBookingTime = mapLatestBookingTime(latestBookingTime, bookWhen);

      if (minimumBookingPeriod != null) {
        issueStore.add(
          "BookingInfoPeriodIgnored",
          "MinimumBookingPeriod cannot be set if latestBookingTime is set. " +
          "MinimumBookingPeriod will be ignored for: %s, entity: %s",
          contactStructure,
          entityRef
        );
      }
    } else if (minimumBookingPeriod != null) {
      minimumBookingNotice = minimumBookingPeriod;
    }

    return new BookingInfo(
      contactInfo,
      bookingMethods,
      otpEarliestBookingTime,
      otpLatestBookingTime,
      minimumBookingNotice,
      Duration.ZERO,
      bookingNote != null ? bookingNote.getValue() : null,
      null,
      null
    );
  }
}
