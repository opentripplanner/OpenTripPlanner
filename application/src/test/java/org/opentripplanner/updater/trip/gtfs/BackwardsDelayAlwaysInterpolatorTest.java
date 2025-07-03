package org.opentripplanner.updater.trip.gtfs;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.OptionalInt;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.opentripplanner.transit.model._data.TimetableRepositoryForTest;
import org.opentripplanner.transit.model.framework.Deduplicator;
import org.opentripplanner.transit.model.timetable.ScheduledTripTimes;
import org.opentripplanner.transit.model.timetable.Trip;
import org.opentripplanner.transit.model.timetable.TripTimesFactory;

class BackwardsDelayAlwaysInterpolatorTest {

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
      new BackwardsDelayAlwaysInterpolator().propagateBackwards(builder)
    );
    // nothing after the first given update should be touched, so it should be left null
    assertNull(builder.getDepartureDelay(0));
  }

  @Test
  void propagateFromIntermediateStop() {
    var firstUpdateIndex = 2;
    var delay = 3;
    var builder = SCHEDULED_TRIP_TIMES.createRealTimeWithoutScheduledTimes()
      .withArrivalDelay(firstUpdateIndex, delay);
    var reference = SCHEDULED_TRIP_TIMES.createRealTimeWithoutScheduledTimes()
      .withArrivalDelay(firstUpdateIndex, delay);
    assertEquals(
      OptionalInt.of(firstUpdateIndex),
      new BackwardsDelayAlwaysInterpolator().propagateBackwards(builder)
    );
    // everything before the first given update should be filled in
    for (var i = 0; i < firstUpdateIndex; ++i) {
      assertEquals(delay, builder.getArrivalDelay(i));
      assertEquals(delay, builder.getDepartureDelay(i));
    }
    // nothing after the first given update should be touched
    for (var i = firstUpdateIndex; i < STOP_COUNT; i++) {
      assertEquals(reference.getArrivalDelay(i), builder.getArrivalDelay(i));
      assertEquals(reference.getDepartureDelay(i), builder.getDepartureDelay(i));
    }
  }

  @Test
  void propagateWithDepartureAsFirstUpdate() {
    var firstUpdateIndex = 2;
    var delay = 3;
    var builder = SCHEDULED_TRIP_TIMES.createRealTimeWithoutScheduledTimes()
      .withDepartureDelay(firstUpdateIndex, delay);
    var reference = SCHEDULED_TRIP_TIMES.createRealTimeWithoutScheduledTimes()
      .withDepartureDelay(firstUpdateIndex, delay);
    assertEquals(
      OptionalInt.of(firstUpdateIndex),
      new BackwardsDelayAlwaysInterpolator().propagateBackwards(builder)
    );
    // everything before the first given update should be filled in
    for (var i = 0; i < firstUpdateIndex; ++i) {
      assertEquals(delay, builder.getArrivalDelay(i));
      assertEquals(delay, builder.getDepartureDelay(i));
    }
    // the arrival should be fill in as well
    assertEquals(delay, builder.getArrivalDelay(firstUpdateIndex));
    // nothing after the first given update should be touched
    for (var i = firstUpdateIndex + 1; i < STOP_COUNT; i++) {
      assertEquals(reference.getArrivalDelay(i), builder.getArrivalDelay(i));
      assertEquals(reference.getDepartureDelay(i), builder.getDepartureDelay(i));
    }
  }

  @Test
  void noUpdatesAtAll() {
    var builder = SCHEDULED_TRIP_TIMES.createRealTimeWithoutScheduledTimes();
    Assertions.assertThrows(IllegalArgumentException.class, () ->
      new BackwardsDelayAlwaysInterpolator().propagateBackwards(builder)
    );
  }
}
