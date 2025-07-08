package org.opentripplanner.transit.model.timetable;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opentripplanner.transit.model._data.TimetableRepositoryForTest.id;
import static org.opentripplanner.transit.model.timetable.TimetableValidationError.ErrorCode.NEGATIVE_DWELL_TIME;
import static org.opentripplanner.transit.model.timetable.TimetableValidationError.ErrorCode.NEGATIVE_HOP_TIME;

import java.util.LinkedList;
import java.util.List;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.opentripplanner.framework.i18n.I18NString;
import org.opentripplanner.framework.i18n.NonLocalizedString;
import org.opentripplanner.model.StopTime;
import org.opentripplanner.transit.model._data.TimetableRepositoryForTest;
import org.opentripplanner.transit.model.framework.DataValidationException;
import org.opentripplanner.transit.model.framework.Deduplicator;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.transit.model.site.RegularStop;

class RealTimeTripTimesTest {

  private static final TimetableRepositoryForTest TEST_MODEL = TimetableRepositoryForTest.of();

  private static final String TRIP_ID = "testTripId";

  private static final List<FeedScopedId> stopIds = List.of(
    id("A"),
    id("B"),
    id("C"),
    id("D"),
    id("E"),
    id("F"),
    id("G"),
    id("H")
  );

  static TripTimes createInitialTripTimes() {
    Trip trip = TimetableRepositoryForTest.trip(TRIP_ID).build();

    List<StopTime> stopTimes = new LinkedList<>();

    for (int i = 0; i < stopIds.size(); ++i) {
      StopTime stopTime = new StopTime();

      RegularStop stop = TEST_MODEL.stop(stopIds.get(i).getId(), 0.0, 0.0).build();
      stopTime.setStop(stop);
      stopTime.setArrivalTime(i * 60);
      stopTime.setDepartureTime(i * 60);
      stopTime.setStopSequence(i * 10);
      stopTimes.add(stopTime);
    }

    return TripTimesFactory.tripTimes(trip, stopTimes, new Deduplicator());
  }

  @Nested
  class Headsign {

    private static final NonLocalizedString STOP_TEST_DIRECTION = new NonLocalizedString(
      "STOP TEST DIRECTION"
    );
    private static final NonLocalizedString DIRECTION = new NonLocalizedString("DIRECTION");
    private static final StopTime EMPTY_STOPPOINT = new StopTime();

    @Test
    void shouldHandleBothNullScenario() {
      Trip trip = TimetableRepositoryForTest.trip("TRIP").build();
      List<StopTime> stopTimes = List.of(EMPTY_STOPPOINT, EMPTY_STOPPOINT, EMPTY_STOPPOINT);

      TripTimes tripTimes = TripTimesFactory.tripTimes(trip, stopTimes, new Deduplicator());

      I18NString headsignFirstStop = tripTimes.getHeadsign(0);
      assertNull(headsignFirstStop);
    }

    @Test
    void shouldHandleTripOnlyHeadSignScenario() {
      Trip trip = TimetableRepositoryForTest.trip("TRIP").withHeadsign(DIRECTION).build();
      List<StopTime> stopTimes = List.of(EMPTY_STOPPOINT, EMPTY_STOPPOINT, EMPTY_STOPPOINT);

      TripTimes tripTimes = TripTimesFactory.tripTimes(trip, stopTimes, new Deduplicator());

      I18NString headsignFirstStop = tripTimes.getHeadsign(0);
      assertEquals(DIRECTION, headsignFirstStop);
    }

    @Test
    void shouldHandleStopsOnlyHeadSignScenario() {
      Trip trip = TimetableRepositoryForTest.trip("TRIP").build();
      StopTime stopWithHeadsign = new StopTime();
      stopWithHeadsign.setStopHeadsign(STOP_TEST_DIRECTION);
      List<StopTime> stopTimes = List.of(stopWithHeadsign, stopWithHeadsign, stopWithHeadsign);

      TripTimes tripTimes = TripTimesFactory.tripTimes(trip, stopTimes, new Deduplicator());

      I18NString headsignFirstStop = tripTimes.getHeadsign(0);
      assertEquals(STOP_TEST_DIRECTION, headsignFirstStop);
    }

    @Test
    void shouldHandleStopsEqualToTripHeadSignScenario() {
      Trip trip = TimetableRepositoryForTest.trip("TRIP").withHeadsign(DIRECTION).build();
      StopTime stopWithHeadsign = new StopTime();
      stopWithHeadsign.setStopHeadsign(DIRECTION);
      List<StopTime> stopTimes = List.of(stopWithHeadsign, stopWithHeadsign, stopWithHeadsign);

      TripTimes tripTimes = TripTimesFactory.tripTimes(trip, stopTimes, new Deduplicator());

      I18NString headsignFirstStop = tripTimes.getHeadsign(0);
      assertEquals(DIRECTION, headsignFirstStop);
    }

    @Test
    void shouldHandleDifferingTripAndStopHeadSignScenario() {
      Trip trip = TimetableRepositoryForTest.trip("TRIP").withHeadsign(DIRECTION).build();
      StopTime stopWithHeadsign = new StopTime();
      stopWithHeadsign.setStopHeadsign(STOP_TEST_DIRECTION);
      List<StopTime> stopTimes = List.of(stopWithHeadsign, EMPTY_STOPPOINT, EMPTY_STOPPOINT);

      TripTimes tripTimes = TripTimesFactory.tripTimes(trip, stopTimes, new Deduplicator());

      I18NString headsignFirstStop = tripTimes.getHeadsign(0);
      assertEquals(STOP_TEST_DIRECTION, headsignFirstStop);

      I18NString headsignSecondStop = tripTimes.getHeadsign(1);
      assertEquals(DIRECTION, headsignSecondStop);
    }
  }

  @Test
  public void testStopUpdate() {
    RealTimeTripTimesBuilder builder = createInitialTripTimes().createRealTimeFromScheduledTimes();

    builder.withArrivalTime(3, 190);
    builder.withDepartureTime(3, 190);
    builder.withArrivalTime(5, 311);
    builder.withDepartureTime(5, 312);

    var updatedTripTimesA = builder.build();

    assertEquals(190, updatedTripTimesA.getArrivalTime(3));
    assertEquals(190, updatedTripTimesA.getDepartureTime(3));
    assertEquals(311, updatedTripTimesA.getArrivalTime(5));
    assertEquals(312, updatedTripTimesA.getDepartureTime(5));
  }

  @Test
  public void testOnlyScheduledTimesAreCopied() {
    var initialTripTimes = createInitialTripTimes();
    var realTimeTripTimes = initialTripTimes
      .createRealTimeFromScheduledTimes()
      .withArrivalDelay(3, -1)
      .build();
    assertEquals(
      initialTripTimes.getArrivalTime(3),
      realTimeTripTimes.createRealTimeFromScheduledTimes().build().getArrivalTime(3)
    );
  }

  @Test
  public void testIncompleteTimes() {
    assertThrows(
      IllegalStateException.class,
      createInitialTripTimes().createRealTimeWithoutScheduledTimes()::build
    );
  }

  @Test
  public void testCompleteTimes() {
    var builder = createInitialTripTimes().createRealTimeWithoutScheduledTimes();
    var delay = 30;
    for (var i = 0; i < builder.numberOfStops(); ++i) {
      builder.withArrivalDelay(i, delay).withDepartureDelay(i, delay);
    }
    var tripTimes = builder.build();
    for (var i = 0; i < tripTimes.getNumStops(); ++i) {
      assertEquals(delay, tripTimes.getArrivalDelay(i));
      assertEquals(delay, tripTimes.getDepartureDelay(i));
    }
  }

  @Test
  public void testPassedUpdate() {
    RealTimeTripTimesBuilder builder = createInitialTripTimes().createRealTimeFromScheduledTimes();

    builder.withDepartureTime(0, 30);
    var updatedTripTimesA = builder.build();

    assertEquals(30, updatedTripTimesA.getDepartureTime(0));
    assertEquals(60, updatedTripTimesA.getArrivalTime(1));
  }

  /**
   * Test negative hop time with stop cancellations.
   * Scheduled: 5 at 300, 6 at 360, 7 at 420
   * Test case: 5 at 421, 6 cancelled (with internal representation 481, since delays are propagated
   * to later stops without arrival or departure), 7 at 420
   * Result: Error to be present at stop 7, due to negative hop time. Error should stay after
   * interpolation, since 6 would be then less than 5 due to interpolation.
   */
  @Test
  public void testNegativeHopTimeWithStopCancellations() {
    var builder = createInitialTripTimes().createRealTimeFromScheduledTimes();

    builder.withDepartureTime(5, 421);
    builder.withArrivalTime(6, 481);
    builder.withDepartureTime(6, 481);
    builder.withCanceled(6);
    builder.withArrivalTime(7, 420);

    var error = assertThrows(DataValidationException.class, builder::build);
    assertEquals(
      "NEGATIVE_HOP_TIME for stop position 7 in trip Trip{F:testTripId RRtestTripId}.",
      error.error().message()
    );
  }

  /**
   * Test positive hop time with stop cancellations when buses run late.
   * Scheduled: 5 at 300, 6 at 360, 7 at 420
   * Test case: 5 at 400, 6 cancelled (with internal representation 460, since delays are propagated
   * to later stops without arrival or departure), 7 at 420.
   * Result: Expect error before interpolation. Expect no errors, after interpolation.
   */
  @Test
  public void testPositiveHopTimeWithStopCancellationsLate() {
    var builder = createInitialTripTimes().createRealTimeFromScheduledTimes();

    builder.withDepartureTime(5, 400);
    builder.withArrivalTime(6, 460);
    builder.withDepartureTime(6, 460);
    builder.withCanceled(6);
    builder.withArrivalTime(7, 420);

    var error = assertThrows(DataValidationException.class, builder::build);
    assertEquals(
      "NEGATIVE_HOP_TIME for stop position 7 in trip Trip{F:testTripId RRtestTripId}.",
      error.error().message()
    );

    assertTrue(builder.interpolateMissingTimes());

    builder.build();
  }

  /**
   * Test positive hop time with stop cancellations when buses run early.
   * Scheduled: 5 at 300, 6 at 360, 7 at 420
   * Test case: 5 at 300, 6 cancelled(with internal representation 360, since delays are propagated
   * to later stops without arrival or departure), 7 at 320.
   * Result: Expect errors, but no errors after interpolation.
   */
  @Test
  public void testPositiveHopTimeWithStopCancellationsEarly() {
    var builder = createInitialTripTimes().createRealTimeFromScheduledTimes();

    builder.withDepartureTime(5, 300);
    builder.withCanceled(6);
    builder.withArrivalTime(7, 320);

    var error = assertThrows(DataValidationException.class, builder::build);
    assertEquals(
      "NEGATIVE_HOP_TIME for stop position 7 in trip Trip{F:testTripId RRtestTripId}.",
      error.error().message()
    );

    assertTrue(builder.interpolateMissingTimes());
    builder.build();
  }

  /**
   * Test positive hop time with stop cancellations at the beginning of the trip.
   * Scheduled: 0 at 0, 1 at 60, 2 at 120, 3 at 180, 4 at 240, 5 at 300, 6 at 360, 7 at 420
   * Test case: 0 and 1 cancelled, start trip at stop 2 at time 0.
   * Result: Expect errors, since 0 and 1 is cancelled and not backward propagated. Expect no
   * interpolation, since there is no times to interpolate before 0. Expect same error after
   * interpolation.
   */
  @Test
  public void testPositiveHopTimeWithTerminalCancellation() {
    var builder = createInitialTripTimes().createRealTimeFromScheduledTimes();

    builder.withCanceled(0);
    builder.withCanceled(1);
    builder.withArrivalTime(2, 0);
    builder.withDepartureTime(2, 10);

    var error = assertThrows(DataValidationException.class, builder::build);
    assertEquals(
      "NEGATIVE_HOP_TIME for stop position 2 in trip Trip{F:testTripId RRtestTripId}.",
      error.error().message()
    );

    assertFalse(builder.interpolateMissingTimes());
    error = assertThrows(DataValidationException.class, builder::build);
    assertEquals(
      "NEGATIVE_HOP_TIME for stop position 2 in trip Trip{F:testTripId RRtestTripId}.",
      error.error().message()
    );
  }

  /**
   * Test positive hop time with stop cancellations at the beginning of the trip.
   * Scheduled: 0 at 0, 1 at 60, 2 at 120, 3 at 180, 4 at 240, 5 at 300, 6 at 360, 7 at 420
   * Test case: 6 and 7 are cancelled.
   * Result: Expect no errors and no interpolations, since there is no time to interpolate at the
   * end terminal.
   */
  @Test
  public void testInterpolationWithTerminalCancellation() {
    var builder = createInitialTripTimes().createRealTimeFromScheduledTimes();

    builder.withCanceled(6);
    builder.withCanceled(7);

    assertFalse(builder.interpolateMissingTimes());

    builder.build();
  }

  /**
   * Test interpolation with multiple cancelled stops together.
   * Scheduled: 0 at 0, 1 at 60, 2 at 120, 3 at 180, 4 at 240, 5 at 300, 6 at 360, 7 at 420
   * Test case: 0 at 0, 1 to 6 are cancelled, 7 at time 350.
   * Result: Expect errors, since 7 is less than the scheduled time at 6. Expect no errors after
   * interpolation.
   */
  @Test
  public void testInterpolationWithMultipleStopCancellations() {
    var builder = createInitialTripTimes().createRealTimeFromScheduledTimes();

    builder.withCanceled(1);
    builder.withCanceled(2);
    builder.withCanceled(3);
    builder.withCanceled(4);
    builder.withCanceled(5);
    builder.withCanceled(6);
    builder.withArrivalTime(7, 350);
    builder.withDepartureTime(7, 350);

    var error = assertThrows(DataValidationException.class, builder::build);
    assertEquals(
      "NEGATIVE_HOP_TIME for stop position 7 in trip Trip{F:testTripId RRtestTripId}.",
      error.error().message()
    );

    assertTrue(builder.interpolateMissingTimes());

    builder.build();
  }

  /**
   * Test interpolation with multiple cancelled stops together.
   * Scheduled: 0 at 0, 1 at 60, 2 at 120, 3 at 180, 4 at 240, 5 at 300, 6 at 360, 7 at 420
   * Test case: 0 at 0, 1 2 cancelled, 3 at 90, 4 5 6 cancelled, 7 at time 240.
   * Result: Expect errors, since 3 is less than scheduled time at 2, and 7 is less than the
   * propagated delay time at 6. Expect no errors after interpolation.
   */
  @Test
  public void testInterpolationWithMultipleStopCancellations2() {
    var builder = createInitialTripTimes().createRealTimeFromScheduledTimes();

    builder.withCanceled(1);
    builder.withCanceled(2);
    builder.withArrivalTime(3, 90);
    builder.withDepartureTime(3, 90);
    builder.withCanceled(4);
    builder.withCanceled(5);
    builder.withCanceled(6);
    builder.withArrivalTime(7, 240);
    builder.withDepartureTime(7, 240);

    var error = assertThrows(DataValidationException.class, builder::build);
    assertEquals(
      "NEGATIVE_HOP_TIME for stop position 3 in trip Trip{F:testTripId RRtestTripId}.",
      error.error().message()
    );

    assertTrue(builder.interpolateMissingTimes());
    builder.build();
  }

  @Test
  public void testNonIncreasingUpdateCrossingMidnight() {
    var builder = createInitialTripTimes().createRealTimeFromScheduledTimes();

    builder.withArrivalTime(0, -300); //"Yesterday"
    builder.withDepartureTime(0, 50);

    builder.build();
  }

  @Test
  public void testDelay() {
    var builder = createInitialTripTimes().createRealTimeFromScheduledTimes();
    builder.withDepartureDelay(0, 10);
    builder.withArrivalDelay(6, 13);

    assertEquals(10, builder.getDepartureTime(0));
    assertEquals(6 * 60 + 13, builder.getArrivalTime(6));
  }

  @Test
  public void testCancel() {
    var builder = createInitialTripTimes().createRealTimeFromScheduledTimes();
    builder.cancelTrip();
    assertEquals(RealTimeState.CANCELED, builder.build().getRealTimeState());
  }

  @Test
  public void testNoData() {
    var builder = createInitialTripTimes().createRealTimeFromScheduledTimes();
    builder.withNoData(1);
    var updatedTripTimesA = builder.build();
    assertFalse(updatedTripTimesA.isNoDataStop(0));
    assertTrue(updatedTripTimesA.isNoDataStop(1));
    assertFalse(updatedTripTimesA.isNoDataStop(2));
  }

  @Test
  public void testRealTimeUpdated() {
    var builder = createInitialTripTimes().createRealTimeFromScheduledTimes();
    assertFalse(builder.build().isRealTimeUpdated(1));
    builder.withRealTimeState(RealTimeState.UPDATED);
    assertTrue(builder.build().isRealTimeUpdated(1));
    builder.withNoData(1);
    var updatedTripTimesA = builder.build();
    assertTrue(updatedTripTimesA.isRealTimeUpdated(0));
    assertFalse(updatedTripTimesA.isRealTimeUpdated(1));
  }

  @Nested
  class GtfsStopSequence {

    @Test
    void gtfsSequence() {
      var stopIndex = createInitialTripTimes().gtfsSequenceOfStopIndex(2);
      assertEquals(20, stopIndex);
    }

    @Test
    void stopIndexOfGtfsSequence() {
      var stopIndex = createInitialTripTimes().stopIndexOfGtfsSequence(40);
      assertTrue(stopIndex.isPresent());
      assertEquals(4, stopIndex.getAsInt());
    }

    @Test
    void unknownGtfsSequence() {
      var stopIndex = createInitialTripTimes().stopIndexOfGtfsSequence(4);
      assertTrue(stopIndex.isEmpty());
    }
  }

  @Test
  public void validateNegativeDwellTime() {
    var expMsg = "NEGATIVE_DWELL_TIME for stop position 3 in trip Trip{F:testTripId RRtestTripId}.";
    var tt = createInitialTripTimes();
    var updatedTt = tt.createRealTimeFromScheduledTimes();

    updatedTt.withArrivalTime(3, 69);
    updatedTt.withDepartureTime(3, 68);

    var ex = assertThrows(DataValidationException.class, updatedTt::build);
    var error = (TimetableValidationError) ex.error();

    assertEquals(3, error.stopIndex());
    assertEquals(NEGATIVE_DWELL_TIME, error.code());
    assertEquals(expMsg, error.message());
    assertEquals(expMsg, ex.getMessage());
  }

  @Test
  public void validateNegativeHopTime() {
    var expMsg = "NEGATIVE_HOP_TIME for stop position 2 in trip Trip{F:testTripId RRtestTripId}.";
    var tt = createInitialTripTimes();
    var updatedTt = tt.createRealTimeFromScheduledTimes();

    updatedTt.withDepartureTime(1, 100);
    updatedTt.withArrivalTime(2, 99);

    var ex = assertThrows(DataValidationException.class, updatedTt::build);
    var error = (TimetableValidationError) ex.error();

    assertEquals(2, error.stopIndex());
    assertEquals(NEGATIVE_HOP_TIME, error.code());
    assertEquals(expMsg, error.message());
    assertEquals(expMsg, ex.getMessage());
  }
}
