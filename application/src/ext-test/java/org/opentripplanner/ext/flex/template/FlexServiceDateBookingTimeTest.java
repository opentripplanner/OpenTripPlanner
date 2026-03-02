package org.opentripplanner.ext.flex.template;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import org.junit.jupiter.api.Test;
import org.opentripplanner.transit.model.timetable.booking.RoutingBookingInfo;

/**
 * Tests for {@link FlexServiceDate#requestedBookingTime()} to verify that booking time
 * is correctly calculated relative to each service date's start-of-service.
 */
class FlexServiceDateBookingTimeTest {

  private static final ZoneId ZONE = ZoneId.of("Europe/Oslo");

  @Test
  void testRequestedBookingTimeSameDay() {
    // Booking at 14:00 on Jan 13, service date is Jan 13
    LocalDate serviceDate = LocalDate.of(2026, 1, 13);
    Instant bookingTime = ZonedDateTime.of(serviceDate, LocalTime.of(14, 0), ZONE).toInstant();

    FlexServiceDate flexDate = FlexServiceDate.of(serviceDate, 0, bookingTime, ZONE, null);

    // Expected: 14:00 = 14 * 3600 = 50400 seconds from start of service
    int expected = 14 * 3600;
    assertEquals(expected, flexDate.requestedBookingTime());
  }

  @Test
  void testRequestedBookingTimePreviousDay() {
    // Booking at 14:40 on Jan 12, service date is Jan 13
    // This is the bug scenario: booking time should be negative relative to Jan 13
    LocalDate bookingDate = LocalDate.of(2026, 1, 12);
    LocalDate serviceDate = LocalDate.of(2026, 1, 13);

    Instant bookingTime = ZonedDateTime.of(bookingDate, LocalTime.of(14, 40), ZONE).toInstant();

    FlexServiceDate flexDate = FlexServiceDate.of(serviceDate, 0, bookingTime, ZONE, null);

    // Booking is at 14:40 on Jan 12
    // Service date start is midnight Jan 13 (NOON - 12h)
    // 14:40 on Jan 12 is 9 hours 20 minutes before midnight Jan 13
    // = -(9*3600 + 20*60) = -33600 seconds
    int expected = -(9 * 3600 + 20 * 60);
    assertEquals(expected, flexDate.requestedBookingTime());
  }

  @Test
  void testRequestedBookingTimeMultipleDaysAhead() {
    // Booking at 10:00 on Jan 10, service date is Jan 13
    LocalDate bookingDate = LocalDate.of(2026, 1, 10);
    LocalDate serviceDate = LocalDate.of(2026, 1, 13);

    Instant bookingTime = ZonedDateTime.of(bookingDate, LocalTime.of(10, 0), ZONE).toInstant();

    FlexServiceDate flexDate = FlexServiceDate.of(serviceDate, 0, bookingTime, ZONE, null);

    // 10:00 on Jan 10 is 2 days + 14 hours before midnight Jan 13
    // = -(2*24 + 14) hours = -62 hours = -62 * 3600 seconds = -223200
    int expected = -((2 * 24 + 14) * 3600);
    assertEquals(expected, flexDate.requestedBookingTime());
  }

  @Test
  void testRequestedBookingTimeNull() {
    LocalDate serviceDate = LocalDate.of(2026, 1, 13);

    FlexServiceDate flexDate = FlexServiceDate.of(serviceDate, 0, null, ZONE, null);

    assertEquals(RoutingBookingInfo.NOT_SET, flexDate.requestedBookingTime());
  }

  @Test
  void testBookingTimeOnDifferentDatesProducesDifferentResults() {
    // Same booking instant should produce different requestedBookingTime values
    // for different service dates - this is the core fix
    Instant bookingTime = ZonedDateTime.of(
      LocalDate.of(2026, 1, 12),
      LocalTime.of(14, 40),
      ZONE
    ).toInstant();

    FlexServiceDate jan12 = FlexServiceDate.of(
      LocalDate.of(2026, 1, 12),
      0,
      bookingTime,
      ZONE,
      null
    );

    FlexServiceDate jan13 = FlexServiceDate.of(
      LocalDate.of(2026, 1, 13),
      0,
      bookingTime,
      ZONE,
      null
    );

    // On Jan 12: booking at 14:40 = 14*3600 + 40*60 = 52800 seconds
    assertEquals(14 * 3600 + 40 * 60, jan12.requestedBookingTime());

    // On Jan 13: booking at 14:40 on Jan 12 = -9*3600 - 20*60 = -33600 seconds
    assertEquals(-(9 * 3600 + 20 * 60), jan13.requestedBookingTime());

    // They must be different!
    assertEquals(
      86400,
      jan12.requestedBookingTime() - jan13.requestedBookingTime(),
      "Booking time difference should equal one day in seconds"
    );
  }
}
