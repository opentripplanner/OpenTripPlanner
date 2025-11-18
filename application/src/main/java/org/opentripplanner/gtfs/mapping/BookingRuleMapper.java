package org.opentripplanner.gtfs.mapping;

import java.time.Duration;
import java.time.LocalTime;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.Nullable;
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

  @Nullable
  private BookingTime earliestBookingTime(BookingRule rule) {
    return resolveBookingTime(rule.getPriorNoticeStartTime(), rule.getPriorNoticeStartDay());
  }

  @Nullable
  private BookingTime latestBookingTime(BookingRule rule) {
    return resolveBookingTime(rule.getPriorNoticeLastTime(), rule.getPriorNoticeLastDay());
  }

  /**
   * If GTFS does not specify the latest booking time/day, the underlying values default to NO_VALUE.
   * In that case, we do not set the booking time so that min/max booking notice can apply.
   *
   * @return null if either timeSeconds or day are NO_VALUE, otherwise a BookingTime instance
   */
  @Nullable
  private BookingTime resolveBookingTime(int timeSeconds, int day) {
    if (timeSeconds == BookingRule.NO_VALUE || day == BookingRule.NO_VALUE) {
      return null;
    }

    return new BookingTime(LocalTime.ofSecondOfDay(timeSeconds), day);
  }

  @Nullable
  private Duration minimumBookingNotice(BookingRule rule) {
    return resolveNoticePeriod(rule.getPriorNoticeDurationMin());
  }

  @Nullable
  private Duration maximumBookingNotice(BookingRule rule) {
    return resolveNoticePeriod(rule.getPriorNoticeDurationMax());
  }

  @Nullable
  private static Duration resolveNoticePeriod(int minutes) {
    if (minutes == BookingRule.NO_VALUE) {
      return null;
    }
    return Duration.ofMinutes(minutes);
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
