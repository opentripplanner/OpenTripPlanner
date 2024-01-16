package org.opentripplanner.routing.algorithm.raptoradapter.transit;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opentripplanner._support.time.ZoneIds;
import org.opentripplanner.ext.flex.FlexAccessEgress;
import org.opentripplanner.ext.flex.FlexPathDurations;
import org.opentripplanner.ext.flex.trip.FlexTrip;
import org.opentripplanner.model.BookingInfo;
import org.opentripplanner.model.BookingInfoBuilder;
import org.opentripplanner.model.BookingTime;
import org.opentripplanner.model.StopTime;
import org.opentripplanner.raptor.api.model.RaptorConstants;
import org.opentripplanner.street.search.state.State;
import org.opentripplanner.street.search.state.TestStateBuilder;
import org.opentripplanner.transit.model._data.TransitModelForTest;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.transit.model.site.RegularStop;

class OpeningHoursAdjusterTest {

  private static final TransitModelForTest TEST_MODEL = TransitModelForTest.of();
  private static final RegularStop S1 = TEST_MODEL.stop("STOP1", 0.0, 0.0).build();
  private static final RegularStop S2 = TEST_MODEL.stop("STOP2", 1.0, 1.0).build();
  private static final FeedScopedId TRIP_ID = new FeedScopedId("FEED", "ID");
  private static final LocalDate REQUEST_DATE = LocalDate.of(2024, 1, 1);
  private static final LocalDateTime REQUEST_DATE_TIME = REQUEST_DATE.atTime(8, 0, 0);
  private static final Instant REQUEST_INSTANT = REQUEST_DATE_TIME.atZone(ZoneIds.UTC).toInstant();

  private static final LocalDate REQUEST_DATE_DST_FORWARD = LocalDate.of(2024, 3, 31);
  private static final LocalDateTime REQUEST_DATE_TIME_DST_FORWARD = REQUEST_DATE_DST_FORWARD.atTime(
    8,
    0,
    0
  );
  private static final Instant REQUEST_INSTANT_DST_FORWARD = REQUEST_DATE_TIME_DST_FORWARD
    .atZone(ZoneIds.OSLO)
    .toInstant();

  private static final LocalDate REQUEST_DATE_DST_BACKWARD = LocalDate.of(2024, 10, 27);
  private static final LocalDateTime REQUEST_DATE_TIME_DST_BACKWARD = REQUEST_DATE_DST_BACKWARD.atTime(
    8,
    0,
    0
  );
  private static final Instant REQUEST_INSTANT_DST_BACKWARD = REQUEST_DATE_TIME_DST_BACKWARD
    .atZone(ZoneIds.OSLO)
    .toInstant();

  private static final BookingInfo BOOK_BEFORE_11_H_45 = new BookingInfoBuilder()
    .withLatestBookingTime(new BookingTime(LocalTime.of(11, 45, 0), 0))
    .build();

  private static final BookingInfo BOOK_BEFORE_21_H_00_PREVIOUS_DAY = new BookingInfoBuilder()
    .withLatestBookingTime(new BookingTime(LocalTime.of(21, 0, 0), 1))
    .build();
  private static final BookingInfo BOOK_ONE_HOUR_BEFORE = new BookingInfoBuilder()
    .withMinimumBookingNotice(Duration.ofHours(1))
    .build();
  public static final int SECONDS_IN_TWELVE_HOURS = 12 * 60 * 60;

  private FlexAccessEgressAdapter accessEgress;
  private int defaultPickupTime;

  @BeforeEach
  void setup() {
    State state = TestStateBuilder.ofWalking().streetEdge().streetEdge().build();

    StopTime stopTime1 = new StopTime();
    stopTime1.setStop(S1);
    stopTime1.setFlexWindowStart(LocalTime.of(12, 12).toSecondOfDay());
    stopTime1.setFlexWindowEnd(LocalTime.of(14, 12).toSecondOfDay());
    StopTime stopTime2 = new StopTime();
    stopTime2.setStop(S2);
    stopTime2.setFlexWindowStart(LocalTime.of(14, 11).toSecondOfDay());
    stopTime2.setFlexWindowEnd(LocalTime.of(14, 12).toSecondOfDay());

    FlexTrip trip = TEST_MODEL.unscheduledTrip(TRIP_ID, List.of(stopTime1, stopTime2));
    FlexAccessEgress flexAccessEgress = new FlexAccessEgress(
      S1,
      new FlexPathDurations(0, 1165, 156, 0),
      0,
      1,
      trip,
      state,
      true
    );
    accessEgress = new FlexAccessEgressAdapter(flexAccessEgress, false);
    defaultPickupTime = accessEgress.earliestDepartureTime(0);
  }

  @Test
  void testEarliestBookingTimeBeforeLatestBookingTime() {
    Instant beforeLatestBookingTime = REQUEST_DATE
      .atTime(BOOK_BEFORE_11_H_45.getLatestBookingTime().getTime().minusMinutes(1))
      .atZone(ZoneIds.UTC)
      .toInstant();
    OpeningHoursAdjuster openingHoursAdjuster = new OpeningHoursAdjuster(
      BOOK_BEFORE_11_H_45,
      accessEgress,
      beforeLatestBookingTime,
      REQUEST_INSTANT,
      ZoneIds.UTC
    );
    assertEquals(
      defaultPickupTime,
      openingHoursAdjuster.earliestDepartureTime(0),
      "When booking before the latest booking time, the default pick-up time is used"
    );
  }

  @Test
  void testEarliestBookingTimeBeforeLatestBookingTimeOnPreviousDay() {
    Instant beforeLatestBookingTime = REQUEST_DATE
      .minusDays(1)
      .atTime(BOOK_BEFORE_21_H_00_PREVIOUS_DAY.getLatestBookingTime().getTime().minusMinutes(1))
      .atZone(ZoneIds.UTC)
      .toInstant();
    OpeningHoursAdjuster openingHoursAdjuster = new OpeningHoursAdjuster(
      BOOK_BEFORE_21_H_00_PREVIOUS_DAY,
      accessEgress,
      beforeLatestBookingTime,
      REQUEST_INSTANT,
      ZoneIds.UTC
    );
    assertEquals(
      defaultPickupTime,
      openingHoursAdjuster.earliestDepartureTime(0),
      "When booking before the latest booking time, the default pick-up time is used"
    );
  }

  @Test
  void testEarliestBookingTimeAfterLatestBookingTime() {
    Instant afterLatestBookingTime = REQUEST_DATE
      .atTime(BOOK_BEFORE_11_H_45.getLatestBookingTime().getTime().plusMinutes(1))
      .atZone(ZoneIds.UTC)
      .toInstant();
    OpeningHoursAdjuster openingHoursAdjuster = new OpeningHoursAdjuster(
      BOOK_BEFORE_11_H_45,
      accessEgress,
      afterLatestBookingTime,
      REQUEST_INSTANT,
      ZoneIds.UTC
    );
    assertEquals(
      RaptorConstants.TIME_NOT_SET,
      openingHoursAdjuster.earliestDepartureTime(0),
      "When booking after the latest booking time, the trip cannot be boarded"
    );
  }

  @Test
  void testEarliestBookingTimeAfterLatestBookingTimeOnPreviousDay() {
    Instant afterLatestBookingTime = REQUEST_DATE
      .minusDays(1)
      .atTime(BOOK_BEFORE_21_H_00_PREVIOUS_DAY.getLatestBookingTime().getTime().plusMinutes(1))
      .atZone(ZoneIds.UTC)
      .toInstant();
    OpeningHoursAdjuster openingHoursAdjuster = new OpeningHoursAdjuster(
      BOOK_BEFORE_21_H_00_PREVIOUS_DAY,
      accessEgress,
      afterLatestBookingTime,
      REQUEST_INSTANT,
      ZoneIds.UTC
    );
    assertEquals(
      RaptorConstants.TIME_NOT_SET,
      openingHoursAdjuster.earliestDepartureTime(0),
      "When booking after the latest booking time, the trip cannot be boarded"
    );
  }

  @Test
  void testEarliestBookingTimeBeforeMinimumBookingNotice() {
    Instant beforeMinimumBookingNotice = REQUEST_DATE
      .atTime(
        LocalTime
          .ofSecondOfDay(defaultPickupTime)
          .minus(BOOK_ONE_HOUR_BEFORE.getMinimumBookingNotice())
          .minusMinutes(1)
      )
      .atZone(ZoneIds.UTC)
      .toInstant();
    OpeningHoursAdjuster openingHoursAdjuster = new OpeningHoursAdjuster(
      BOOK_ONE_HOUR_BEFORE,
      accessEgress,
      beforeMinimumBookingNotice,
      REQUEST_INSTANT,
      ZoneIds.UTC
    );
    assertEquals(
      defaultPickupTime,
      openingHoursAdjuster.earliestDepartureTime(0),
      "When booking before the minimum booking notice, the default pick-up time is used"
    );
  }

  @Test
  void testEarliestBookingTimeAfterMinimumBookingNotice() {
    Instant afterMinimumBookingNotice = REQUEST_DATE
      .atTime(
        LocalTime
          .ofSecondOfDay(defaultPickupTime)
          .minus(BOOK_ONE_HOUR_BEFORE.getMinimumBookingNotice())
          .plusMinutes(2)
      )
      .atZone(ZoneIds.UTC)
      .toInstant();
    OpeningHoursAdjuster openingHoursAdjuster = new OpeningHoursAdjuster(
      BOOK_ONE_HOUR_BEFORE,
      accessEgress,
      afterMinimumBookingNotice,
      REQUEST_INSTANT,
      ZoneIds.UTC
    );
    assertEquals(
      RaptorConstants.TIME_NOT_SET,
      openingHoursAdjuster.earliestDepartureTime(0),
      "When booking after the minimum booking notice, the trip cannot be boarded"
    );
  }

  @Test
  void testConvertEarliestBookingTimeToOtpTimeOnNormalDate() {
    // 12:00:00 on 2024-01-01 in Oslo occurs 12 hours after 00:00:00
    Instant earliestBookingTime = REQUEST_DATE.atTime(12, 0, 0).atZone(ZoneIds.OSLO).toInstant();
    assertEquals(
      SECONDS_IN_TWELVE_HOURS,
      OpeningHoursAdjuster.convertEarliestBookingTimeToOtpTime(
        earliestBookingTime,
        REQUEST_INSTANT,
        ZoneIds.OSLO
      )
    );
  }

  @Test
  void testConvertEarliestBookingTimeToOtpTimeOnDSTForward() {
    // 12:00:00 on 2024-03-31 in Oslo occurs 11 hours after 00:00:00
    // OTP time starts at 23:00:00 on 2024-03-30
    Instant earliestBookingTime = REQUEST_DATE_DST_FORWARD
      .atTime(12, 0, 0)
      .atZone(ZoneIds.OSLO)
      .toInstant();
    assertEquals(
      SECONDS_IN_TWELVE_HOURS,
      OpeningHoursAdjuster.convertEarliestBookingTimeToOtpTime(
        earliestBookingTime,
        REQUEST_INSTANT_DST_FORWARD,
        ZoneIds.OSLO
      )
    );
  }

  @Test
  void testConvertEarliestBookingTimeToOtpTimeOnDSTBackward() {
    // 12:00:00 on 2024-10-27 in Oslo occurs 13 hours after 00:00:00
    // OTP time starts at 01:00:00 on 2024-10-27
    Instant earliestBookingTime = REQUEST_DATE_DST_FORWARD
      .atTime(12, 0, 0)
      .atZone(ZoneIds.OSLO)
      .toInstant();
    assertEquals(
      SECONDS_IN_TWELVE_HOURS,
      OpeningHoursAdjuster.convertEarliestBookingTimeToOtpTime(
        earliestBookingTime,
        REQUEST_INSTANT_DST_FORWARD,
        ZoneIds.OSLO
      )
    );
  }
}
