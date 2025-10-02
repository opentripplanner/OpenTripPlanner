package org.opentripplanner.gtfs.mapping;

import java.time.Duration;
import java.time.LocalTime;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.gtfs.model.BookingRule;
import org.opentripplanner.transit.model.organization.ContactInfo;
import org.opentripplanner.transit.model.timetable.booking.BookingInfo;
import org.opentripplanner.transit.model.timetable.booking.BookingMethod;
import org.opentripplanner.transit.model.timetable.booking.BookingTime;

/** Responsible for mapping GTFS BookingRule into the OTP model. */
class BookingRuleMapper {

  private final Map<AgencyAndId, BookingInfo> cachedBookingInfos = new HashMap<>();

  /** Map from GTFS to OTP model, {@code null} safe. */
  BookingInfo map(BookingRule rule) {
    if (rule == null) {
      return null;
    }

    return cachedBookingInfos.computeIfAbsent(rule.getId(), k ->
      BookingInfo.of()
        .withContactInfo(contactInfo(rule))
        .withBookingMethods(bookingMethods())
        .withEarliestBookingTime(earliestBookingTime(rule))
        .withLatestBookingTime(latestBookingTime(rule))
        .withMinimumBookingNotice(minimumBookingNotice(rule))
        .withMaximumBookingNotice(maximumBookingNotice(rule))
        .withMessage(message(rule))
        .withPickupMessage(pickupMessage(rule))
        .withDropOffMessage(dropOffMessage(rule))
        .build()
    );
  }

  private ContactInfo contactInfo(BookingRule rule) {
    return ContactInfo.of()
      .withPhoneNumber(rule.getPhoneNumber())
      .withInfoUrl(rule.getInfoUrl())
      .withBookingUrl(rule.getUrl())
      .build();
  }

  private EnumSet<BookingMethod> bookingMethods() {
    return null;
  }

  private BookingTime earliestBookingTime(BookingRule rule) {
    int startTimeSeconds = rule.getPriorNoticeStartTime();
    int startDay = rule.getPriorNoticeStartDay();

    // If GTFS does not specify the earliest booking time/day, the underlying values default to 0.
    // In that case, do not set earliestBookingTime so that min/max booking notice can apply.
    if (startTimeSeconds == 0 && startDay == 0) {
      return null;
    }

    return new BookingTime(LocalTime.ofSecondOfDay(startTimeSeconds), startDay);
  }

  private BookingTime latestBookingTime(BookingRule rule) {
    int lastTimeSeconds = rule.getPriorNoticeLastTime();
    int lastDay = rule.getPriorNoticeLastDay();

    // If GTFS does not specify the latest booking time/day, the underlying values default to 0.
    // In that case, do not set latestBookingTime so that min/max booking notice can apply.
    if (lastTimeSeconds == 0 && lastDay == 0) {
      return null;
    }

    return new BookingTime(LocalTime.ofSecondOfDay(lastTimeSeconds), lastDay);
  }

  private Duration minimumBookingNotice(BookingRule rule) {
    return Duration.ofSeconds(rule.getPriorNoticeDurationMin());
  }

  private Duration maximumBookingNotice(BookingRule rule) {
    return Duration.ofSeconds(rule.getPriorNoticeDurationMax());
  }

  private String message(BookingRule rule) {
    return rule.getMessage();
  }

  private String pickupMessage(BookingRule rule) {
    return rule.getPickupMessage();
  }

  private String dropOffMessage(BookingRule rule) {
    return rule.getDropOffMessage();
  }
}
