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
    RealTimeTripTimes updatedTripTimesA = createInitialTripTimes().copyScheduledTimes();

    updatedTripTimesA.updateArrivalTime(3, 190);
    updatedTripTimesA.updateDepartureTime(3, 190);
    updatedTripTimesA.updateArrivalTime(5, 311);
    updatedTripTimesA.updateDepartureTime(5, 312);

    assertEquals(3 * 60 + 10, updatedTripTimesA.getArrivalTime(3));
    assertEquals(3 * 60 + 10, updatedTripTimesA.getDepartureTime(3));
    assertEquals(5 * 60 + 11, updatedTripTimesA.getArrivalTime(5));
    assertEquals(5 * 60 + 12, updatedTripTimesA.getDepartureTime(5));
  }

  @Test
  public void testPassedUpdate() {
    RealTimeTripTimes updatedTripTimesA = createInitialTripTimes().copyScheduledTimes();

    updatedTripTimesA.updateDepartureTime(0, 30);

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
    var updatedTripTimes = createInitialTripTimes().copyScheduledTimes();

    updatedTripTimes.updateDepartureTime(5, 421);
    updatedTripTimes.updateArrivalTime(6, 481);
    updatedTripTimes.updateDepartureTime(6, 481);
    updatedTripTimes.setCancelled(6);
    updatedTripTimes.updateArrivalTime(7, 420);

    var error = assertThrows(
      DataValidationException.class,
      updatedTripTimes::validateNonIncreasingTimes
    );
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
    var updatedTripTimes = createInitialTripTimes().copyScheduledTimes();

    updatedTripTimes.updateDepartureTime(5, 400);
    updatedTripTimes.updateArrivalTime(6, 460);
    updatedTripTimes.updateDepartureTime(6, 460);
    updatedTripTimes.setCancelled(6);
    updatedTripTimes.updateArrivalTime(7, 420);

    var error = assertThrows(
      DataValidationException.class,
      updatedTripTimes::validateNonIncreasingTimes
    );
    assertEquals(
      "NEGATIVE_HOP_TIME for stop position 7 in trip Trip{F:testTripId RRtestTripId}.",
      error.error().message()
    );

    assertTrue(updatedTripTimes.interpolateMissingTimes());

    updatedTripTimes.validateNonIncreasingTimes();
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
    var updatedTripTimes = createInitialTripTimes().copyScheduledTimes();

    updatedTripTimes.updateDepartureTime(5, 300);
    updatedTripTimes.setCancelled(6);
    updatedTripTimes.updateArrivalTime(7, 320);

    var error = assertThrows(
      DataValidationException.class,
      updatedTripTimes::validateNonIncreasingTimes
    );
    assertEquals(
      "NEGATIVE_HOP_TIME for stop position 7 in trip Trip{F:testTripId RRtestTripId}.",
      error.error().message()
    );

    assertTrue(updatedTripTimes.interpolateMissingTimes());
    updatedTripTimes.validateNonIncreasingTimes();
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
    var updatedTripTimes = createInitialTripTimes().copyScheduledTimes();

    updatedTripTimes.setCancelled(0);
    updatedTripTimes.setCancelled(1);
    updatedTripTimes.updateArrivalTime(2, 0);
    updatedTripTimes.updateDepartureTime(2, 10);

    var error = assertThrows(
      DataValidationException.class,
      updatedTripTimes::validateNonIncreasingTimes
    );
    assertEquals(
      "NEGATIVE_HOP_TIME for stop position 2 in trip Trip{F:testTripId RRtestTripId}.",
      error.error().message()
    );

    assertFalse(updatedTripTimes.interpolateMissingTimes());
    error = assertThrows(
      DataValidationException.class,
      updatedTripTimes::validateNonIncreasingTimes
    );
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
    var updatedTripTimes = createInitialTripTimes().copyScheduledTimes();

    updatedTripTimes.setCancelled(6);
    updatedTripTimes.setCancelled(7);

    assertFalse(updatedTripTimes.interpolateMissingTimes());

    updatedTripTimes.validateNonIncreasingTimes();
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
    var updatedTripTimes = createInitialTripTimes().copyScheduledTimes();

    updatedTripTimes.setCancelled(1);
    updatedTripTimes.setCancelled(2);
    updatedTripTimes.setCancelled(3);
    updatedTripTimes.setCancelled(4);
    updatedTripTimes.setCancelled(5);
    updatedTripTimes.setCancelled(6);
    updatedTripTimes.updateArrivalTime(7, 350);
    updatedTripTimes.updateDepartureTime(7, 350);

    var error = assertThrows(
      DataValidationException.class,
      updatedTripTimes::validateNonIncreasingTimes
    );
    assertEquals(
      "NEGATIVE_HOP_TIME for stop position 7 in trip Trip{F:testTripId RRtestTripId}.",
      error.error().message()
    );

    assertTrue(updatedTripTimes.interpolateMissingTimes());

    updatedTripTimes.validateNonIncreasingTimes();
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
    var updatedTripTimes = createInitialTripTimes().copyScheduledTimes();

    updatedTripTimes.setCancelled(1);
    updatedTripTimes.setCancelled(2);
    updatedTripTimes.updateArrivalTime(3, 90);
    updatedTripTimes.updateDepartureTime(3, 90);
    updatedTripTimes.setCancelled(4);
    updatedTripTimes.setCancelled(5);
    updatedTripTimes.setCancelled(6);
    updatedTripTimes.updateArrivalTime(7, 240);
    updatedTripTimes.updateDepartureTime(7, 240);

    var error = assertThrows(
      DataValidationException.class,
      updatedTripTimes::validateNonIncreasingTimes
    );
    assertEquals(
      "NEGATIVE_HOP_TIME for stop position 3 in trip Trip{F:testTripId RRtestTripId}.",
      error.error().message()
    );

    assertTrue(updatedTripTimes.interpolateMissingTimes());
    updatedTripTimes.validateNonIncreasingTimes();
  }

  @Test
  public void testNonIncreasingUpdateCrossingMidnight() {
    RealTimeTripTimes updatedTripTimesA = createInitialTripTimes().copyScheduledTimes();

    updatedTripTimesA.updateArrivalTime(0, -300); //"Yesterday"
    updatedTripTimesA.updateDepartureTime(0, 50);

    updatedTripTimesA.validateNonIncreasingTimes();
  }

  @Test
  public void testDelay() {
    RealTimeTripTimes updatedTripTimesA = createInitialTripTimes().copyScheduledTimes();
    updatedTripTimesA.updateDepartureDelay(0, 10);
    updatedTripTimesA.updateArrivalDelay(6, 13);

    assertEquals(10, updatedTripTimesA.getDepartureTime(0));
    assertEquals(6 * 60 + 13, updatedTripTimesA.getArrivalTime(6));
  }

  @Test
  public void testCancel() {
    RealTimeTripTimes updatedTripTimesA = createInitialTripTimes().copyScheduledTimes();
    updatedTripTimesA.cancelTrip();
    assertEquals(RealTimeState.CANCELED, updatedTripTimesA.getRealTimeState());
  }

  @Test
  public void testNoData() {
    RealTimeTripTimes updatedTripTimesA = createInitialTripTimes().copyScheduledTimes();
    updatedTripTimesA.setNoData(1);
    assertFalse(updatedTripTimesA.isNoDataStop(0));
    assertTrue(updatedTripTimesA.isNoDataStop(1));
    assertFalse(updatedTripTimesA.isNoDataStop(2));
  }

  @Test
  public void testRealTimeUpdated() {
    RealTimeTripTimes updatedTripTimesA = createInitialTripTimes().copyScheduledTimes();
    assertFalse(updatedTripTimesA.isRealTimeUpdated(1));
    updatedTripTimesA.setRealTimeState(RealTimeState.UPDATED);
    assertTrue(updatedTripTimesA.isRealTimeUpdated(1));
    updatedTripTimesA.setNoData(1);
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
    var updatedTt = tt.copyScheduledTimes();

    updatedTt.updateArrivalTime(3, 69);
    updatedTt.updateDepartureTime(3, 68);

    var ex = assertThrows(DataValidationException.class, updatedTt::validateNonIncreasingTimes);
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
    var updatedTt = tt.copyScheduledTimes();

    updatedTt.updateDepartureTime(1, 100);
    updatedTt.updateArrivalTime(2, 99);

    var ex = assertThrows(DataValidationException.class, updatedTt::validateNonIncreasingTimes);
    var error = (TimetableValidationError) ex.error();

    assertEquals(2, error.stopIndex());
    assertEquals(NEGATIVE_HOP_TIME, error.code());
    assertEquals(expMsg, error.message());
    assertEquals(expMsg, ex.getMessage());
  }
}
