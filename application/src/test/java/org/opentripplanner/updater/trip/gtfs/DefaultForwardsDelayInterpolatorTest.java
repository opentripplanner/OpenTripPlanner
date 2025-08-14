package org.opentripplanner.updater.trip.gtfs;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.OptionalInt;
import org.junit.jupiter.api.Test;
import org.opentripplanner.transit.model._data.TimetableRepositoryForTest;
import org.opentripplanner.transit.model.framework.Deduplicator;
import org.opentripplanner.transit.model.timetable.ScheduledTripTimes;
import org.opentripplanner.transit.model.timetable.StopRealTimeState;
import org.opentripplanner.transit.model.timetable.Trip;
import org.opentripplanner.transit.model.timetable.TripTimesFactory;

class DefaultForwardsDelayInterpolatorTest {

  static final Trip TRIP = TimetableRepositoryForTest.trip("TRIP_ID").build();
  static final int STOP_COUNT = 20;
  static final ScheduledTripTimes SCHEDULED_TRIP_TIMES = TripTimesFactory.tripTimes(
    TRIP,
    TimetableRepositoryForTest.of().stopTimesEvery5Minutes(STOP_COUNT, TRIP, "00:00"),
    new Deduplicator()
  );

  @Test
  void allUpdatesProvided() {
    var builder = SCHEDULED_TRIP_TIMES.createRealTimeWithoutScheduledTimes();
    for (var i = 0; i < STOP_COUNT; i++) {
      // the service accumulates 1 second of dwell delay at each stop
      builder.withArrivalDelay(i, i);
      builder.withDepartureDelay(i, i + 1);
    }
    assertFalse(new DefaultForwardsDelayInterpolator().interpolateDelay(builder));
    for (var i = 0; i < STOP_COUNT; i++) {
      assertEquals(i, builder.getArrivalDelay(i));
      assertEquals(i + 1, builder.getDepartureDelay(i));
    }
    builder.build();
  }

  // The examples come from https://gtfs.org/documentation/realtime/feed-entities/trip-updates/#stoptimeupdate

  // For a trip with 20 stops, a StopTimeUpdate with arrival delay and departure delay of 0
  // (StopTimeEvents) for stop_sequence of the current stop means that the trip is exactly on time.
  @Test
  void example1() {
    var builder = SCHEDULED_TRIP_TIMES.createRealTimeWithoutScheduledTimes();
    builder.withArrivalDelay(0, 0);
    builder.withDepartureDelay(0, 0);
    assertTrue(new DefaultForwardsDelayInterpolator().interpolateDelay(builder));
    for (var i = 0; i < STOP_COUNT; ++i) {
      assertEquals(0, builder.getArrivalDelay(i));
      assertEquals(0, builder.getDepartureDelay(i));
    }
    builder.build();
  }

  // For the same trip instance, three StopTimeUpdates are provided:
  //
  // delay of 300 seconds for stop_sequence 3
  // delay of 60 seconds for stop_sequence 8
  // ScheduleRelationship of NO_DATA for stop_sequence 10
  // This will be interpreted as:
  //
  // stop_sequences 1,2 have unknown delay.
  // stop_sequences 3,4,5,6,7 have delay of 300 seconds.
  // stop_sequences 8,9 have delay of 60 seconds.
  // stop_sequences 10,..,20 have unknown delay.
  @Test
  void example2() {
    var builder = SCHEDULED_TRIP_TIMES.createRealTimeWithoutScheduledTimes();
    builder.withArrivalDelay(3, 300);
    builder.withArrivalDelay(8, 60);
    builder.withNoData(10);
    assertTrue(new DefaultForwardsDelayInterpolator().interpolateDelay(builder));
    // stops 0 to 2 are handled by the backwards propagator, which is outside the scope of the test
    for (var i = 3; i < 8; ++i) {
      assertEquals(300, builder.getArrivalDelay(i));
      assertEquals(300, builder.getDepartureDelay(i));
    }
    for (var i = 8; i < 9; ++i) {
      assertEquals(60, builder.getArrivalDelay(i));
      assertEquals(60, builder.getDepartureDelay(i));
    }
    for (var i = 10; i < STOP_COUNT; ++i) {
      // for NO_DATA stop, we assume that they run as scheduled in the internal model
      assertEquals(0, builder.getArrivalDelay(i));
      assertEquals(0, builder.getDepartureDelay(i));
      assertEquals(StopRealTimeState.NO_DATA, builder.getStopRealTimeState(i));
    }
    // we need to propagate backwards from stop 3 before building
    assertEquals(
      OptionalInt.of(3),
      new BackwardsDelayRequiredInterpolator(true).propagateBackwards(builder)
    );
    builder.build();
  }

  @Test
  void onlyDepartureTimeIsProvided() {
    var builder = SCHEDULED_TRIP_TIMES.createRealTimeWithoutScheduledTimes();
    builder.withArrivalDelay(0, 450);
    builder.withDepartureDelay(0, 450);
    builder.withDepartureDelay(1, 300);
    assertTrue(new DefaultForwardsDelayInterpolator().interpolateDelay(builder));
    // if the delay at stop 0 is propagated to the arrival at stop 1, it will foul the given data
    // therefore we need to force the interpolated arrival to be not later than the given departure
    assertEquals(450, builder.getDepartureDelay(0));
    assertEquals(300, builder.getDepartureDelay(1));
    assertEquals(builder.getDepartureTime(1), builder.getArrivalTime(1));
    builder.build();
  }

  @Test
  void delayedDepartureThenNoData() {
    var builder = SCHEDULED_TRIP_TIMES.createRealTimeWithoutScheduledTimes();
    builder.withArrivalDelay(0, 450);
    builder.withDepartureDelay(0, 450);
    builder.withNoData(1);
    assertTrue(new DefaultForwardsDelayInterpolator().interpolateDelay(builder));
    // for stop 1, the scheduled time is earlier than the delayed departure at stop 0
    assertNotEquals(0, builder.getArrivalDelay(1));
    assertNotEquals(0, builder.getDepartureDelay(1));
    assertEquals(builder.getDepartureTime(0), builder.getArrivalTime(1));
    assertEquals(builder.getArrivalTime(1), builder.getDepartureTime(1));
    // for stop 2, the scheduled time does not foul the non-decreasing condition
    assertEquals(0, builder.getArrivalDelay(2));
    assertEquals(0, builder.getDepartureDelay(2));
    builder.build();
  }

  @Test
  void delayedDepartureThenNoDataAtLongDwell() {
    var builder = TripTimesFactory.tripTimes(
      TRIP,
      List.of(
        TimetableRepositoryForTest.of().stopTime(TRIP, 0, 0),
        TimetableRepositoryForTest.of().stopTime(TRIP, 1, 300, 600)
      ),
      new Deduplicator()
    ).createRealTimeWithoutScheduledTimes();
    builder.withArrivalDelay(0, 450);
    builder.withDepartureDelay(0, 450);
    builder.withNoData(1);
    assertTrue(new DefaultForwardsDelayInterpolator().interpolateDelay(builder));
    // for stop 1, the scheduled arrival time is earlier than the delayed departure at stop 0
    assertNotEquals(0, builder.getArrivalDelay(1));
    assertEquals(builder.getDepartureTime(0), builder.getArrivalTime(1));
    // however, the scheduled departure time is later than that
    assertEquals(0, builder.getDepartureDelay(1));
    builder.build();
  }

  @Test
  void noRealTimeIsProvided() {
    var builder = SCHEDULED_TRIP_TIMES.createRealTimeWithoutScheduledTimes()
      .withCanceled(5)
      .withInaccuratePredictions(7);
    assertTrue(new DefaultForwardsDelayInterpolator().interpolateDelay(builder));
    for (var i = 0; i < STOP_COUNT; ++i) {
      assertEquals(0, builder.getArrivalDelay(i));
      assertEquals(0, builder.getDepartureDelay(i));
    }
    for (var i = 0; i < 5; ++i) {
      assertEquals(StopRealTimeState.DEFAULT, builder.getStopRealTimeState(i));
    }
    assertEquals(StopRealTimeState.CANCELLED, builder.getStopRealTimeState(5));
    // SKIPPED stop does not propagate
    assertEquals(StopRealTimeState.DEFAULT, builder.getStopRealTimeState(6));
    for (var i = 7; i < STOP_COUNT; ++i) {
      assertEquals(StopRealTimeState.INACCURATE_PREDICTIONS, builder.getStopRealTimeState(i));
    }
    builder.build();
  }

  @Test
  void canceledStops() {
    // Stops 4 to 10 takes 1800 seconds scheduled, 900 seconds actual
    var builder = SCHEDULED_TRIP_TIMES.createRealTimeWithoutScheduledTimes()
      .withCanceled(0)
      .withCanceled(1)
      .withCanceled(2)
      .withCanceled(5)
      .withCanceled(6)
      .withCanceled(7)
      .withCanceled(8)
      .withCanceled(9)
      .withCanceled(18)
      .withCanceled(19)
      .withArrivalTime(3, 3000)
      .withArrivalTime(10, 4200);
    assertTrue(new DefaultForwardsDelayInterpolator().interpolateDelay(builder));
    // 0 to 2 should not be touched at all, it is the job for the backward interpolator
    assertNull(builder.getDepartureTime(0));
    assertNull(builder.getDepartureTime(1));
    assertNull(builder.getDepartureTime(2));
    assertEquals(3000, builder.getDepartureTime(3));
    // for non-canceled stops, the delay is propagated as-is
    assertEquals(3300, builder.getDepartureTime(4));
    // for the skipped stops, the travel time is apportioned
    assertEquals(3450, builder.getDepartureTime(5));
    assertEquals(3600, builder.getDepartureTime(6));
    assertEquals(3750, builder.getDepartureTime(7));
    assertEquals(3900, builder.getDepartureTime(8));
    assertEquals(4050, builder.getDepartureTime(9));
    assertEquals(4200, builder.getDepartureTime(10));
    for (int i = 11; i < STOP_COUNT; ++i) {
      assertEquals(builder.getDepartureDelay(10), builder.getDepartureDelay(i));
    }
    assertEquals(
      OptionalInt.of(3),
      new BackwardsDelayRequiredInterpolator(true).propagateBackwards(builder)
    );
    builder.build();
  }
}
