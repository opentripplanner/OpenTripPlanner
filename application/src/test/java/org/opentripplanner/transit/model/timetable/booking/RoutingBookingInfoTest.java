package org.opentripplanner.transit.model.timetable.booking;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.time.Duration;
import java.time.LocalTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.opentripplanner.utils.time.TimeUtils;

class RoutingBookingInfoTest {

  private static final Duration MINIMUM_BOOKING_NOTICE_20m = Duration.ofMinutes(20);
  private static final LocalTime T13_20 = LocalTime.of(13, 20);
  private static final LocalTime T13_00 = LocalTime.of(13, 0);
  private static final LocalTime T13_00_01 = LocalTime.of(13, 0, 1);
  private static final LocalTime T13_40 = LocalTime.of(13, 40);
  private static final LocalTime T13_40_01 = LocalTime.of(13, 40, 1);
  private static final LocalTime T14_00 = LocalTime.of(14, 0);
  private static final LocalTime LATEST_BOOKING_TIME_13_00 = T13_00;

  static List<Arguments> testCase() {
    //   BOARD-TIME   |   REQUESTED-BOOKING-TIME   |   EXPECTED
    return List.of(
      // Test min-booking-notice <= 13:40  (14:00-20m)
      Arguments.of(T14_00, T13_40, Expect.MIN_BOOKING_NOTICE),
      Arguments.of(T14_00, T13_40_01, Expect.NONE),
      // Test latest-booking-time <= 13_00
      Arguments.of(T13_00, T13_00, Expect.LATEST_BOOKING_TIME),
      Arguments.of(T13_00, T13_00_01, Expect.NONE),
      // Combination of both
      Arguments.of(T13_20, LocalTime.of(13, 0, 0), Expect.BOTH)
    );
  }

  @ParameterizedTest
  @MethodSource("testCase")
  void isThereEnoughTimeToBookWithMinBookingTimeBeforeDeparture(
    LocalTime searchTime,
    LocalTime requestedBookingTime,
    Expect expect
  ) {
    int searchTimeSec = searchTime.toSecondOfDay();

    var subject = RoutingBookingInfo.of(requestedBookingTime.toSecondOfDay())
      .withMinimumBookingNotice(MINIMUM_BOOKING_NOTICE_20m)
      .withLatestBookingTime(new BookingTime(LATEST_BOOKING_TIME_13_00, 0))
      .build();

    // Since we have not set a duration or offset, departure and arrival is the same
    assertEquals(expect.latestBookingTime, !subject.exceedsLatestBookingTime());
    assertEquals(expect.minBookingNotice, !subject.exceedsMinimumBookingNotice(searchTimeSec));
  }

  @Test
  void earliestDepartureTime() {
    int t11_35 = TimeUtils.time("11:35");
    int t11_55 = TimeUtils.time("11:35") + (int) MINIMUM_BOOKING_NOTICE_20m.toSeconds();

    var subject = RoutingBookingInfo.of(t11_35)
      .withMinimumBookingNotice(MINIMUM_BOOKING_NOTICE_20m)
      .build();

    // 11:55 is the earliest departure time for any time before 11:55
    assertEquals(subject.earliestDepartureTime(0), t11_55);
    assertEquals(subject.earliestDepartureTime(t11_55 - 1), t11_55);
    assertEquals(subject.earliestDepartureTime(t11_55), t11_55);
    assertEquals(subject.earliestDepartureTime(t11_55 + 1), t11_55 + 1);
  }

  @Test
  void unrestricted() {
    assertFalse(RoutingBookingInfo.unrestricted().exceedsMinimumBookingNotice(10_000_000));
    assertFalse(RoutingBookingInfo.unrestricted().exceedsMinimumBookingNotice(0));
    assertFalse(RoutingBookingInfo.unrestricted().exceedsLatestBookingTime());

    assertSame(RoutingBookingInfo.unrestricted(), RoutingBookingInfo.of(3600).build());

    assertSame(
      RoutingBookingInfo.unrestricted(),
      RoutingBookingInfo.of(RoutingBookingInfo.NOT_SET)
        .withMinimumBookingNotice(MINIMUM_BOOKING_NOTICE_20m)
        .build()
    );
    assertSame(
      RoutingBookingInfo.unrestricted(),
      RoutingBookingInfo.of(T13_00.toSecondOfDay()).build()
    );
  }

  @Test
  void testToString() {
    var subject = RoutingBookingInfo.of(TimeUtils.time("11:35"))
      .withMinimumBookingNotice(MINIMUM_BOOKING_NOTICE_20m)
      .withLatestBookingTime(new BookingTime(LATEST_BOOKING_TIME_13_00, 0))
      .build();

    assertEquals(
      "RoutingBookingInfo{latestBookingTime: 13:00, minimumBookingNotice: 20m}",
      subject.toString()
    );
  }

  @Test
  void testEqAndHashCode() {
    var subject = RoutingBookingInfo.of(
      TimeUtils.time("11:35"),
      BookingInfo.of().withMinimumBookingNotice(MINIMUM_BOOKING_NOTICE_20m).build()
    );
    var same = RoutingBookingInfo.of(TimeUtils.time("11:35"))
      .withMinimumBookingNotice(MINIMUM_BOOKING_NOTICE_20m)
      .build();

    // Equals
    assertNotSame(subject, same);
    assertEquals(subject, same);
    assertEquals(true, subject.equals(subject));
    assertNotEquals(subject, RoutingBookingInfo.unrestricted());
    assertNotEquals(subject, "");

    // HashCode
    assertEquals(subject.hashCode(), same.hashCode());
    assertNotEquals(subject.hashCode(), RoutingBookingInfo.unrestricted().hashCode());
  }

  enum Expect {
    NONE(false, false),
    MIN_BOOKING_NOTICE(true, false),
    LATEST_BOOKING_TIME(false, true),
    BOTH(true, true);

    final boolean minBookingNotice;
    final boolean latestBookingTime;

    Expect(boolean minBookingNotice, boolean latestBookingTime) {
      this.minBookingNotice = minBookingNotice;
      this.latestBookingTime = latestBookingTime;
    }
  }
}
