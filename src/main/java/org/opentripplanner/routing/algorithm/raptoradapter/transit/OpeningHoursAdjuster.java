package org.opentripplanner.routing.algorithm.raptoradapter.transit;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import org.opentripplanner.model.BookingInfo;
import org.opentripplanner.model.BookingTime;
import org.opentripplanner.raptor.api.model.RaptorConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Adjust the opening hours for a given flex access/egress by taking into account the latest booking
 * time or minimum booking period for the corresponding flex trip.
 */
public class OpeningHoursAdjuster {

  private static final Logger LOG = LoggerFactory.getLogger(OpeningHoursAdjuster.class);

  private static final int DAY_IN_SECONDS = 3600 * 24;

  /**
   * Booking info for the boarding stop.
   */
  private final BookingInfo boardingBookingInfo;

  /**
   * The original access/egress.
   */
  private final FlexAccessEgressAdapter delegate;

  /**
   * The earliest time the passenger can book the trip.
   */
  private final int earliestBookingTime;

  public OpeningHoursAdjuster(
    BookingInfo boardingBookingInfo,
    FlexAccessEgressAdapter delegate,
    Instant earliestBookingTime,
    Instant dateTime,
    ZoneId timeZone
  ) {
    this.boardingBookingInfo = boardingBookingInfo;
    this.delegate = delegate;
    this.earliestBookingTime =
      convertEarliestBookingTimeToOtpTime(earliestBookingTime, dateTime, timeZone);
  }

  /**
   * If the earliest booking time is past the latest booking time, the flex trip cannot
   * be boarded and the method returns RaptorConstants.TIME_NOT_SET.
   * If the earliest booking time is past the minimum booking period, the requested departure
   * time is shifted to ensure a minimum booking period.
   * If this results in a departure time outside the opening hours (i.e. after LatestArrivalTime),
   * then the method returns RaptorConstants.TIME_NOT_SET.
   * Otherwise the requested departure time is returned unchanged.
   */
  public int earliestDepartureTime(int requestedDepartureTime) {
    int edt = delegate.earliestDepartureTime(requestedDepartureTime);
    if (edt == RaptorConstants.TIME_NOT_SET) {
      return RaptorConstants.TIME_NOT_SET;
    }
    BookingTime latestBookingTime = boardingBookingInfo.getLatestBookingTime();
    if (latestBookingTime != null) {
      return adjustDepartureTimeWithLatestBookingTime(edt, latestBookingTime);
    }
    Duration minimumBookingNotice = boardingBookingInfo.getMinimumBookingNotice();
    if (minimumBookingNotice != null) {
      return adjustDepartureTimeWithMinimumBookingNotice(edt, minimumBookingNotice);
    }
    LOG.warn(
      "Missing both latest booking time and minimum booking notice on trip {}. Falling back to default earliest booking time",
      delegate.getFlexTrip().getTrip().getId()
    );
    return edt;
  }

  private int adjustDepartureTimeWithLatestBookingTime(int edt, BookingTime latestBookingTime) {
    int otpLatestBookingTime = convertBookingTimeToOtpTime(
      latestBookingTime.getTime(),
      -latestBookingTime.getDaysPrior()
    );
    if (earliestBookingTime <= otpLatestBookingTime) {
      return edt;
    } else {
      return RaptorConstants.TIME_NOT_SET;
    }
  }

  private int adjustDepartureTimeWithMinimumBookingNotice(int edt, Duration minimumBookingNotice) {
    if (edt >= earliestBookingTime + minimumBookingNotice.toSeconds()) {
      return edt;
    } else {
      // Calculate again the earliest departure time shifted by the minimum booking period.
      // This may result in a requested departure time outside the opening hours
      // in which case RaptorConstants.TIME_NOT_SET is returned.
      return delegate.earliestDepartureTime(
        earliestBookingTime + (int) minimumBookingNotice.toSeconds()
      );
    }
  }

  /**
   * Convert a booking time with day offset to OTP time.
   */
  private static int convertBookingTimeToOtpTime(LocalTime time, int dayOffset) {
    return time.toSecondOfDay() + DAY_IN_SECONDS * dayOffset;
  }

  /**
   * Convert the earliest booking time to OTP time.
   * The OTP time starts at midnight the day of the requested dateTime for the requested time zone.
   */
  private static int convertEarliestBookingTimeToOtpTime(
    Instant earliestBookingTime,
    Instant dateTime,
    ZoneId timeZone
  ) {
    ZonedDateTime zonedDateTime = ZonedDateTime.ofInstant(dateTime, timeZone);
    ZonedDateTime zonedEarliestBookingTime = ZonedDateTime.ofInstant(earliestBookingTime, timeZone);
    int days = zonedDateTime.toLocalDate().until(zonedEarliestBookingTime.toLocalDate()).getDays();
    return zonedEarliestBookingTime.toLocalTime().toSecondOfDay() + days * DAY_IN_SECONDS;
  }
}
