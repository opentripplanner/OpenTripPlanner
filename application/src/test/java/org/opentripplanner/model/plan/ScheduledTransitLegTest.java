package org.opentripplanner.model.plan;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opentripplanner.transit.model._data.TimetableRepositoryForTest.id;

import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.opentripplanner._support.time.ZoneIds;
import org.opentripplanner.transit.model._data.TimetableRepositoryForTest;
import org.opentripplanner.transit.model.network.Route;
import org.opentripplanner.transit.model.network.TripPattern;
import org.opentripplanner.transit.model.timetable.RealTimeTripTimes;
import org.opentripplanner.transit.model.timetable.ScheduledTripTimes;
import org.opentripplanner.transit.model.timetable.Trip;

class ScheduledTransitLegTest {

  static final ZonedDateTime TIME = OffsetDateTime
    .parse("2023-04-17T17:49:06+02:00")
    .toZonedDateTime();
  private static final TimetableRepositoryForTest TEST_MODEL = TimetableRepositoryForTest.of();
  private static final Route ROUTE = TimetableRepositoryForTest.route(id("2")).build();
  private static final TripPattern PATTERN = TimetableRepositoryForTest
    .tripPattern("1", ROUTE)
    .withStopPattern(TEST_MODEL.stopPattern(3))
    .build();
  private static final Trip TRIP = TimetableRepositoryForTest.trip("trip1").build();
  private static final ScheduledTripTimes TRIP_TIMES = ScheduledTripTimes
    .of()
    .withArrivalTimes("10:00 11:00 12:00")
    .withDepartureTimes("10:01 11:02 12:03")
    .withTrip(TRIP)
    .build();

  @Test
  void defaultFares() {
    var leg = builder().build();

    assertEquals(List.of(), leg.fareProducts());
  }

  @Test
  void legTimesWithoutRealTime() {
    var leg = builder().withTripTimes(TRIP_TIMES).build();

    assertNull(leg.start().estimated());
    assertNull(leg.end().estimated());
    assertFalse(leg.isRealTimeUpdated());
  }

  @Test
  void legTimesWithRealTime() {
    var tt = ScheduledTripTimes
      .of()
      .withArrivalTimes("10:00 11:00 12:00")
      .withDepartureTimes("10:01 11:02 12:03")
      .withTrip(TRIP)
      .build();

    var rtt = RealTimeTripTimes.of(tt);
    rtt.updateArrivalTime(0, 111);

    var leg = builder().withTripTimes(rtt).build();

    assertNotNull(leg.start().estimated());
    assertNotNull(leg.end().estimated());
    assertTrue(leg.isRealTimeUpdated());
  }

  private static ScheduledTransitLegBuilder builder() {
    return new ScheduledTransitLegBuilder()
      .withTripTimes(null)
      .withTripPattern(PATTERN)
      .withBoardStopIndexInPattern(0)
      .withAlightStopIndexInPattern(2)
      .withStartTime(TIME)
      .withEndTime(TIME.plusMinutes(10))
      .withServiceDate(TIME.toLocalDate())
      .withZoneId(ZoneIds.BERLIN)
      .withGeneralizedCost(100)
      .withDistanceMeters(1000);
  }
}
