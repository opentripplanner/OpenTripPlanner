package org.opentripplanner.transit.model.timetable.booking;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.opentripplanner.transit.model.timetable.booking.RoutingBookingInfoTest.Expected.LATEST_BOOKING_TIME;
import static org.opentripplanner.transit.model.timetable.booking.RoutingBookingInfoTest.Expected.MIN_BOOKING_NOTICE;

import java.time.LocalTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.opentripplanner.framework.time.DurationUtils;

class RoutingBookingInfoTest {

  private static final int MINIMUM_BOOKING_NOTICE_20m = DurationUtils.durationInSeconds("20m");
  private static final LocalTime T13_20 = LocalTime.of(13, 20);
  private static final LocalTime T13_00 = LocalTime.of(13, 0);
  private static final LocalTime T14_00 = LocalTime.of(14, 0);
  private static final LocalTime LATEST_BOOKING_TIME_13_00 = T13_00;

  // NeTEx does not support booking notice and latest booking time, but there is no conflict
  // between these parameters. To make sure we support all combinations, we create all 3:
  private final RoutingBookingInfo subjectBoth = RoutingBookingInfo
    .of()
    .withMinimumBookingNotice(MINIMUM_BOOKING_NOTICE_20m)
    .withLatestBookingTime(LATEST_BOOKING_TIME_13_00.toSecondOfDay())
    .build();

  private final RoutingBookingInfo subjectMinBookingNotice = RoutingBookingInfo
    .of()
    .withMinimumBookingNotice(MINIMUM_BOOKING_NOTICE_20m)
    .build();

  private final RoutingBookingInfo subjectLatestBookingTime = RoutingBookingInfo
    .of()
    .withLatestBookingTime(LATEST_BOOKING_TIME_13_00.toSecondOfDay())
    .build();

  static List<Arguments> testCase() {
    //   BOARD-TIME   |   REQUESTED-BOOKING-TIME   |   EXPECTED
    List<Arguments> arguments = List.of(
      // Test min-booking-notice <= 13:40  (14:00-20m)
      Arguments.of(T14_00, LocalTime.of(13, 39, 59), MIN_BOOKING_NOTICE),
      Arguments.of(T14_00, LocalTime.of(13, 40, 0), MIN_BOOKING_NOTICE),
      Arguments.of(T14_00, LocalTime.of(13, 40, 1), Expected.NONE),
      // Test latest-booking-time <= 13_00
      Arguments.of(T13_00, LocalTime.of(12, 59, 59), LATEST_BOOKING_TIME),
      Arguments.of(T13_00, LocalTime.of(13, 0, 0), LATEST_BOOKING_TIME),
      Arguments.of(T13_00, LocalTime.of(13, 0, 1), Expected.NONE),
      // Combination of both
      Arguments.of(T13_20, LocalTime.of(13, 0, 0), Expected.BOTH)
    );
    return arguments;
  }

  @ParameterizedTest
  @MethodSource("testCase")
  void isThereEnoughTimeToBookWithMinBookingTime(
    LocalTime searchTime,
    LocalTime requestedBookingTime,
    Expected expected
  ) {
    int searchTimeSec = searchTime.toSecondOfDay();
    int reqBookingTime = requestedBookingTime.toSecondOfDay();

    assertEquals(
      expected.minBookingNotice,
      subjectMinBookingNotice.isThereEnoughTimeToBook(searchTimeSec, reqBookingTime)
    );
  }

  @ParameterizedTest
  @MethodSource("testCase")
  void isThereEnoughTimeToBookWithLatestBookingTime(
    LocalTime searchTime,
    LocalTime requestedBookingTime,
    Expected expected
  ) {
    int searchTimeSec = searchTime.toSecondOfDay();
    int reqBookingTime = requestedBookingTime.toSecondOfDay();

    assertEquals(
      expected.latestBookingTime,
      subjectLatestBookingTime.isThereEnoughTimeToBook(searchTimeSec, reqBookingTime)
    );
  }

  @ParameterizedTest
  @MethodSource("testCase")
  void isThereEnoughTimeToBookUsingBoth(
    LocalTime searchTime,
    LocalTime requestedBookingTime,
    Expected expected
  ) {
    int searchTimeSec = searchTime.toSecondOfDay();
    int reqBookingTime = requestedBookingTime.toSecondOfDay();

    assertEquals(
      expected == Expected.BOTH,
      subjectBoth.isThereEnoughTimeToBook(searchTimeSec, reqBookingTime)
    );
  }

  @Test
  void testToString() {
    assertEquals(
      "RoutingBookingInfo{minimumBookingNotice: 20m}",
      subjectMinBookingNotice.toString()
    );
    assertEquals(
      "RoutingBookingInfo{latestBookingTime: 13:00}",
      subjectLatestBookingTime.toString()
    );
    assertEquals(
      "RoutingBookingInfo{latestBookingTime: 13:00, minimumBookingNotice: 20m}",
      subjectBoth.toString()
    );
  }

  enum Expected {
    NONE(false, false),
    MIN_BOOKING_NOTICE(true, false),
    LATEST_BOOKING_TIME(false, true),
    BOTH(true, true);

    boolean minBookingNotice;
    boolean latestBookingTime;

    Expected(boolean minBookingNotice, boolean latestBookingTime) {
      this.minBookingNotice = minBookingNotice;
      this.latestBookingTime = latestBookingTime;
    }
  }
}
