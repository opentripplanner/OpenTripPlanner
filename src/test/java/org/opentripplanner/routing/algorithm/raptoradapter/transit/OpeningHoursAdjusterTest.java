package org.opentripplanner.routing.algorithm.raptoradapter.transit;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
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
  private static final ZoneId TIME_ZONE_UTC = ZoneIds.UTC;
  private static final FeedScopedId TRIP_ID = new FeedScopedId("FEED", "ID");
  private static final LocalDate REQUEST_DATE = LocalDate.of(2024, 1, 1);
  private static final LocalDateTime REQUEST_DATE_TIME = REQUEST_DATE.atTime(8, 0, 0);
  public static final ZoneOffset ZONE_OFFSET_UTC = ZoneOffset.UTC;
  private static final Instant REQUEST_INSTANT = REQUEST_DATE_TIME.toInstant(ZONE_OFFSET_UTC);
  private static final BookingInfo BOOK_BEFORE_11_H_45 = new BookingInfoBuilder()
    .withLatestBookingTime(new BookingTime(LocalTime.of(11, 45, 0), 0))
    .build();
  private static final BookingInfo BOOK_ONE_HOUR_BEFORE = new BookingInfoBuilder()
    .withMinimumBookingNotice(Duration.ofHours(1))
    .build();

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
      .toInstant(ZONE_OFFSET_UTC);
    OpeningHoursAdjuster openingHoursAdjuster = new OpeningHoursAdjuster(
      BOOK_BEFORE_11_H_45,
      accessEgress,
      beforeLatestBookingTime,
      REQUEST_INSTANT,
      TIME_ZONE_UTC
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
      .toInstant(ZONE_OFFSET_UTC);
    OpeningHoursAdjuster openingHoursAdjuster = new OpeningHoursAdjuster(
      BOOK_BEFORE_11_H_45,
      accessEgress,
      afterLatestBookingTime,
      REQUEST_INSTANT,
      TIME_ZONE_UTC
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
      .toInstant(ZONE_OFFSET_UTC);
    OpeningHoursAdjuster openingHoursAdjuster = new OpeningHoursAdjuster(
      BOOK_ONE_HOUR_BEFORE,
      accessEgress,
      beforeMinimumBookingNotice,
      REQUEST_INSTANT,
      TIME_ZONE_UTC
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
      .toInstant(ZONE_OFFSET_UTC);
    OpeningHoursAdjuster openingHoursAdjuster = new OpeningHoursAdjuster(
      BOOK_ONE_HOUR_BEFORE,
      accessEgress,
      afterMinimumBookingNotice,
      REQUEST_INSTANT,
      TIME_ZONE_UTC
    );
    assertEquals(
      RaptorConstants.TIME_NOT_SET,
      openingHoursAdjuster.earliestDepartureTime(0),
      "When booking after the minimum booking notice, the trip cannot be boarded"
    );
  }
}
