package org.opentripplanner.updater.trip.gtfs;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.OptionalInt;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.opentripplanner.transit.model._data.TimetableRepositoryForTest;
import org.opentripplanner.transit.model.framework.Deduplicator;
import org.opentripplanner.transit.model.timetable.ScheduledTripTimes;
import org.opentripplanner.transit.model.timetable.StopRealTimeState;
import org.opentripplanner.transit.model.timetable.Trip;
import org.opentripplanner.transit.model.timetable.TripTimesFactory;

class BackwardsDelayRequiredInterpolatorTest {

  static final Trip TRIP = TimetableRepositoryForTest.trip("TRIP_ID").build();
  public static final int STOP_COUNT = 5;
  static final ScheduledTripTimes SCHEDULED_TRIP_TIMES = TripTimesFactory.tripTimes(
    TRIP,
    TimetableRepositoryForTest.of().stopTimesEvery5Minutes(STOP_COUNT, TRIP, "00:00"),
    new Deduplicator()
  );

  @Test
  void noPropagation() {
    var builder = SCHEDULED_TRIP_TIMES.createRealTimeWithoutScheduledTimes()
      .withArrivalDelay(0, -3);
    assertEquals(
      OptionalInt.empty(),
      new BackwardsDelayRequiredInterpolator(false).propagateBackwards(builder)
    );
    // nothing after the first given update should be touched, so it should be left null
    assertNull(builder.getDepartureDelay(0));
  }

  @Test
  void propagateFromIntermediateStopWithPositiveDelay() {
    var firstUpdateIndex = 2;
    var delay = 3;
    var builder = SCHEDULED_TRIP_TIMES.createRealTimeWithoutScheduledTimes()
      .withArrivalDelay(firstUpdateIndex, delay);
    var reference = SCHEDULED_TRIP_TIMES.createRealTimeWithoutScheduledTimes()
      .withArrivalDelay(firstUpdateIndex, delay);
    assertEquals(
      OptionalInt.of(firstUpdateIndex),
      new BackwardsDelayRequiredInterpolator(false).propagateBackwards(builder)
    );
    // everything before the first given update should be given the scheduled time
    for (var i = 0; i < firstUpdateIndex; ++i) {
      assertEquals(0, builder.getArrivalDelay(i));
      assertEquals(0, builder.getDepartureDelay(i));
      assertEquals(StopRealTimeState.DEFAULT, builder.getStopRealTimeState(i));
    }
    // nothing after the first given update should be touched
    for (var i = firstUpdateIndex; i < STOP_COUNT; i++) {
      assertEquals(reference.getArrivalDelay(i), builder.getArrivalDelay(i));
      assertEquals(reference.getDepartureDelay(i), builder.getDepartureDelay(i));
      assertEquals(StopRealTimeState.DEFAULT, builder.getStopRealTimeState(i));
    }
  }

  @Test
  void propagateFromIntermediateStopWithPositiveDelayAndNoData() {
    var firstUpdateIndex = 2;
    var delay = 3;
    var builder = SCHEDULED_TRIP_TIMES.createRealTimeWithoutScheduledTimes()
      .withArrivalDelay(firstUpdateIndex, delay);
    var reference = SCHEDULED_TRIP_TIMES.createRealTimeWithoutScheduledTimes()
      .withArrivalDelay(firstUpdateIndex, delay);
    assertEquals(
      OptionalInt.of(firstUpdateIndex),
      new BackwardsDelayRequiredInterpolator(true).propagateBackwards(builder)
    );
    // everything before the first given update should be given the scheduled time
    for (var i = 0; i < firstUpdateIndex; ++i) {
      assertEquals(0, builder.getArrivalDelay(i));
      assertEquals(0, builder.getDepartureDelay(i));
      assertEquals(StopRealTimeState.NO_DATA, builder.getStopRealTimeState(i));
    }
    // nothing after the first given update should be touched
    for (var i = firstUpdateIndex; i < STOP_COUNT; i++) {
      assertEquals(reference.getArrivalDelay(i), builder.getArrivalDelay(i));
      assertEquals(reference.getDepartureDelay(i), builder.getDepartureDelay(i));
      assertEquals(StopRealTimeState.DEFAULT, builder.getStopRealTimeState(i));
    }
  }

  @Test
  void propagateThroughCanceledStopWithNoData() {
    var firstUpdateIndex = 2;
    var canceledIndex = 1;
    var delay = 3;
    var builder = SCHEDULED_TRIP_TIMES.createRealTimeWithoutScheduledTimes()
      .withArrivalDelay(firstUpdateIndex, delay)
      .withCanceled(canceledIndex);
    assertEquals(
      OptionalInt.of(firstUpdateIndex),
      new BackwardsDelayRequiredInterpolator(true).propagateBackwards(builder)
    );
    // everything before the first given update should be given the scheduled time and set NO_DATA
    // unless the stop has been canceled
    for (var i = 0; i < firstUpdateIndex; ++i) {
      assertEquals(0, builder.getArrivalDelay(i));
      assertEquals(0, builder.getDepartureDelay(i));
      assertEquals(
        i == canceledIndex ? StopRealTimeState.CANCELLED : StopRealTimeState.NO_DATA,
        builder.getStopRealTimeState(i)
      );
    }
  }

  @Test
  void propagateFromIntermediateStopWithNegativeDelay() {
    // The journey takes 5 minutes to travel a stop, and it arrives stop position 2 at 2.5 minutes
    // Therefore we must propagate the earliness back such that the vehicle started "on time" at
    // the origin, arrived and departed at stop 1 at 2.5 minutes to make the time non-decreasing
    var builder = SCHEDULED_TRIP_TIMES.createRealTimeWithoutScheduledTimes()
      .withArrivalTime(2, 150);
    assertEquals(
      OptionalInt.of(2),
      new BackwardsDelayRequiredInterpolator(true).propagateBackwards(builder)
    );
    assertEquals(0, builder.getArrivalTime(0));
    assertEquals(0, builder.getDepartureTime(0));
    assertEquals(StopRealTimeState.NO_DATA, builder.getStopRealTimeState(0));
    assertEquals(150, builder.getArrivalTime(1));
    assertEquals(150, builder.getDepartureTime(1));
    assertEquals(StopRealTimeState.NO_DATA, builder.getStopRealTimeState(1));
    assertEquals(150, builder.getArrivalTime(2));
    assertNull(builder.getDepartureTime(2));
    assertEquals(StopRealTimeState.DEFAULT, builder.getStopRealTimeState(2));
  }

  @Test
  void propagateWithDepartureAsFirstUpdateAndNoData() {
    var firstUpdateIndex = 2;
    var delay = 3;
    var builder = SCHEDULED_TRIP_TIMES.createRealTimeWithoutScheduledTimes()
      .withDepartureDelay(firstUpdateIndex, delay);
    var reference = SCHEDULED_TRIP_TIMES.createRealTimeWithoutScheduledTimes()
      .withDepartureDelay(firstUpdateIndex, delay);
    assertEquals(
      OptionalInt.of(firstUpdateIndex),
      new BackwardsDelayRequiredInterpolator(true).propagateBackwards(builder)
    );
    // everything before the first given update should be given the scheduled time
    for (var i = 0; i < firstUpdateIndex; ++i) {
      assertEquals(0, builder.getArrivalDelay(i));
      assertEquals(0, builder.getDepartureDelay(i));
      assertEquals(StopRealTimeState.NO_DATA, builder.getStopRealTimeState(i));
    }
    // the arrival should be fill in as well
    assertEquals(0, builder.getArrivalDelay(firstUpdateIndex));
    // TODO: It is not possible to specify NO_DATA just for the arrival yet
    assertEquals(StopRealTimeState.DEFAULT, builder.getStopRealTimeState(firstUpdateIndex));
    // nothing after the first given update should be touched
    for (var i = firstUpdateIndex + 1; i < STOP_COUNT; i++) {
      assertEquals(reference.getArrivalDelay(i), builder.getArrivalDelay(i));
      assertEquals(reference.getDepartureDelay(i), builder.getDepartureDelay(i));
      assertEquals(StopRealTimeState.DEFAULT, builder.getStopRealTimeState(i));
    }
  }

  @Test
  void noUpdatesAtAll() {
    var builder = SCHEDULED_TRIP_TIMES.createRealTimeWithoutScheduledTimes();
    Assertions.assertThrows(IllegalArgumentException.class, () ->
      new BackwardsDelayRequiredInterpolator(false).propagateBackwards(builder)
    );
  }
}
