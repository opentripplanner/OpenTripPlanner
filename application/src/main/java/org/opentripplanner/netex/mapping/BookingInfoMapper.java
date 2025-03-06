package org.opentripplanner.netex.mapping;

import java.time.Duration;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import org.opentripplanner.graph_builder.issue.api.DataImportIssueStore;
import org.opentripplanner.transit.model.organization.ContactInfo;
import org.opentripplanner.transit.model.timetable.booking.BookingInfo;
import org.opentripplanner.transit.model.timetable.booking.BookingMethod;
import org.opentripplanner.transit.model.timetable.booking.BookingTime;
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
    return new NetexBookingInfoBuilder()
      .withFlexibleLine(flexibleLine)
      .withServiceJourney(serviceJourney)
      .withStopPoint(stopPoint)
      .build();
  }

  private class NetexBookingInfoBuilder {

    private ContactStructure bookingContact;
    private List<BookingMethodEnumeration> bookingMethods = new ArrayList<>();
    private LocalTime latestBookingTime;
    private PurchaseWhenEnumeration bookWhen;
    private Duration minimumBookingPeriod;
    private MultilingualString bookingNote;
    private boolean hasBookingInfo;
    private String flexibleLineRef;
    private String serviceJourneyRef;
    private String stopPointRef;

    private NetexBookingInfoBuilder withFlexibleLine(FlexibleLine flexibleLine) {
      if (flexibleLine != null) {
        this.hasBookingInfo = true;
        this.flexibleLineRef = ref("FlexibleLine", flexibleLine);
        setIfNotEmpty(
          flexibleLine.getBookingContact(),
          flexibleLine.getBookingMethods(),
          flexibleLine.getLatestBookingTime(),
          flexibleLine.getBookWhen(),
          flexibleLine.getMinimumBookingPeriod(),
          flexibleLine.getBookingNote()
        );
      }
      return this;
    }

    private NetexBookingInfoBuilder withServiceJourney(ServiceJourney serviceJourney) {
      if (serviceJourney != null && serviceJourney.getFlexibleServiceProperties() != null) {
        this.hasBookingInfo = true;
        this.serviceJourneyRef = ref("ServiceJourney", serviceJourney);
        FlexibleServiceProperties flexibleServiceProperties =
          serviceJourney.getFlexibleServiceProperties();
        setIfNotEmpty(
          flexibleServiceProperties.getBookingContact(),
          flexibleServiceProperties.getBookingMethods(),
          flexibleServiceProperties.getLatestBookingTime(),
          flexibleServiceProperties.getBookWhen(),
          flexibleServiceProperties.getMinimumBookingPeriod(),
          flexibleServiceProperties.getBookingNote()
        );
      }
      return this;
    }

    private NetexBookingInfoBuilder withStopPoint(StopPointInJourneyPattern stopPoint) {
      BookingArrangementsStructure bookingArrangements = stopPoint.getBookingArrangements();
      if (bookingArrangements != null) {
        this.hasBookingInfo = true;
        this.stopPointRef = ref("StopPoint", stopPoint);
        setIfNotEmpty(
          bookingArrangements.getBookingContact(),
          bookingArrangements.getBookingMethods(),
          bookingArrangements.getLatestBookingTime(),
          bookingArrangements.getBookWhen(),
          bookingArrangements.getMinimumBookingPeriod(),
          bookingArrangements.getBookingNote()
        );
      }
      return this;
    }

    private BookingInfo build() {
      if (!hasBookingInfo) {
        return null;
      }
      String entityRefs = flexibleLineRef + '/' + serviceJourneyRef + '/' + stopPointRef;
      return build(
        bookingContact,
        bookingMethods,
        latestBookingTime,
        bookWhen,
        minimumBookingPeriod,
        bookingNote,
        entityRefs
      );
    }

    private static BookingTime mapLatestBookingTime(
      LocalTime latestBookingTime,
      PurchaseWhenEnumeration purchaseWhen
    ) {
      return switch (purchaseWhen) {
        case UNTIL_PREVIOUS_DAY -> new BookingTime(latestBookingTime, 1);
        case DAY_OF_TRAVEL_ONLY, ADVANCE_ONLY, ADVANCE_AND_DAY_OF_TRAVEL -> new BookingTime(
          latestBookingTime,
          0
        );
        case TIME_OF_TRAVEL_ONLY -> null;
        default -> throw new IllegalArgumentException("Value not supported: " + purchaseWhen);
      };
    }

    private static BookingTime mapEarliestBookingTime(PurchaseWhenEnumeration purchaseWhen) {
      return switch (purchaseWhen) {
        case UNTIL_PREVIOUS_DAY,
          ADVANCE_ONLY,
          ADVANCE_AND_DAY_OF_TRAVEL,
          TIME_OF_TRAVEL_ONLY -> null;
        case DAY_OF_TRAVEL_ONLY -> new BookingTime(LocalTime.MIDNIGHT, 0);
        default -> throw new IllegalArgumentException("Value not supported: " + purchaseWhen);
      };
    }

    private static String ref(String type, EntityStructure entity) {
      return type + "(" + entity.getId() + ")";
    }

    private BookingInfo build(
      ContactStructure contactStructure,
      List<BookingMethodEnumeration> bookingMethodEnum,
      LocalTime latestBookingTime,
      PurchaseWhenEnumeration bookWhen,
      Duration minimumBookingPeriod,
      MultilingualString bookingNote,
      String entityRefs
    ) {
      if (contactStructure == null) {
        return null;
      }

      ContactInfo contactInfo = ContactInfo.of()
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

      EnumSet<BookingMethod> filteredBookingMethods = bookingMethodEnum
        .stream()
        .map(bm -> BookingMethodMapper.map(entityRefs, bm))
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
            "MinimumBookingPeriod will be ignored for: %s, entities: %s",
            contactStructure,
            entityRefs
          );
        }
      } else if (minimumBookingPeriod != null) {
        minimumBookingNotice = minimumBookingPeriod;
      }

      String bookingInfoMessage = bookingNote != null ? bookingNote.getValue() : null;
      return BookingInfo.of()
        .withContactInfo(contactInfo)
        .withBookingMethods(filteredBookingMethods)
        .withEarliestBookingTime(otpEarliestBookingTime)
        .withLatestBookingTime(otpLatestBookingTime)
        .withMinimumBookingNotice(minimumBookingNotice)
        .withMaximumBookingNotice(Duration.ZERO)
        .withMessage(bookingInfoMessage)
        .build();
    }

    private void setIfNotEmpty(
      ContactStructure bookingContact,
      List<BookingMethodEnumeration> bookingMethods,
      LocalTime latestBookingTime,
      PurchaseWhenEnumeration bookWhen,
      Duration minimumBookingPeriod,
      MultilingualString bookingNote
    ) {
      if (bookingMethods != null && !bookingMethods.isEmpty()) {
        this.bookingMethods = bookingMethods;
      }
      this.bookingContact = getOrDefault(bookingContact, this.bookingContact);
      this.minimumBookingPeriod = getOrDefault(minimumBookingPeriod, this.minimumBookingPeriod);
      this.latestBookingTime = getOrDefault(latestBookingTime, this.latestBookingTime);
      this.bookWhen = getOrDefault(bookWhen, this.bookWhen);
      this.bookingNote = getOrDefault(bookingNote, this.bookingNote);
    }
  }

  private static <T> T getOrDefault(T value, T defaultValue) {
    return value == null ? defaultValue : value;
  }
}
