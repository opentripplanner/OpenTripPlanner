package org.opentripplanner.transit.model.timetable;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opentripplanner.transit.model._data.TransitModelForTest.id;
import static org.opentripplanner.transit.model.timetable.ValidationError.ErrorCode.NEGATIVE_DWELL_TIME;
import static org.opentripplanner.transit.model.timetable.ValidationError.ErrorCode.NEGATIVE_HOP_TIME;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.opentripplanner.framework.i18n.I18NString;
import org.opentripplanner.framework.i18n.NonLocalizedString;
import org.opentripplanner.model.StopTime;
import org.opentripplanner.transit.model._data.TransitModelForTest;
import org.opentripplanner.transit.model.framework.Deduplicator;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.transit.model.site.RegularStop;

class TripTimesTest {

  private static final String TRIP_ID = "testTripId";

  private static final List<FeedScopedId> stops = List.of(
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
    Trip trip = TransitModelForTest.trip(TRIP_ID).build();

    List<StopTime> stopTimes = new LinkedList<>();

    for (int i = 0; i < stops.size(); ++i) {
      StopTime stopTime = new StopTime();

      RegularStop stop = TransitModelForTest.stopForTest(stops.get(i).getId(), 0.0, 0.0);
      stopTime.setStop(stop);
      stopTime.setArrivalTime(i * 60);
      stopTime.setDepartureTime(i * 60);
      stopTime.setStopSequence(i * 10);
      stopTimes.add(stopTime);
    }

    return new TripTimes(trip, stopTimes, new Deduplicator());
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
      Trip trip = TransitModelForTest.trip("TRIP").build();
      Collection<StopTime> stopTimes = List.of(EMPTY_STOPPOINT, EMPTY_STOPPOINT, EMPTY_STOPPOINT);

      TripTimes tripTimes = new TripTimes(trip, stopTimes, new Deduplicator());

      I18NString headsignFirstStop = tripTimes.getHeadsign(0);
      assertNull(headsignFirstStop);
    }

    @Test
    void shouldHandleTripOnlyHeadSignScenario() {
      Trip trip = TransitModelForTest.trip("TRIP").withHeadsign(DIRECTION).build();
      Collection<StopTime> stopTimes = List.of(EMPTY_STOPPOINT, EMPTY_STOPPOINT, EMPTY_STOPPOINT);

      TripTimes tripTimes = new TripTimes(trip, stopTimes, new Deduplicator());

      I18NString headsignFirstStop = tripTimes.getHeadsign(0);
      assertEquals(DIRECTION, headsignFirstStop);
    }

    @Test
    void shouldHandleStopsOnlyHeadSignScenario() {
      Trip trip = TransitModelForTest.trip("TRIP").build();
      StopTime stopWithHeadsign = new StopTime();
      stopWithHeadsign.setStopHeadsign(STOP_TEST_DIRECTION);
      Collection<StopTime> stopTimes = List.of(
        stopWithHeadsign,
        stopWithHeadsign,
        stopWithHeadsign
      );

      TripTimes tripTimes = new TripTimes(trip, stopTimes, new Deduplicator());

      I18NString headsignFirstStop = tripTimes.getHeadsign(0);
      assertEquals(STOP_TEST_DIRECTION, headsignFirstStop);
    }

    @Test
    void shouldHandleStopsEqualToTripHeadSignScenario() {
      Trip trip = TransitModelForTest.trip("TRIP").withHeadsign(DIRECTION).build();
      StopTime stopWithHeadsign = new StopTime();
      stopWithHeadsign.setStopHeadsign(DIRECTION);
      Collection<StopTime> stopTimes = List.of(
        stopWithHeadsign,
        stopWithHeadsign,
        stopWithHeadsign
      );

      TripTimes tripTimes = new TripTimes(trip, stopTimes, new Deduplicator());

      I18NString headsignFirstStop = tripTimes.getHeadsign(0);
      assertEquals(DIRECTION, headsignFirstStop);
    }

    @Test
    void shouldHandleDifferingTripAndStopHeadSignScenario() {
      Trip trip = TransitModelForTest.trip("TRIP").withHeadsign(DIRECTION).build();
      StopTime stopWithHeadsign = new StopTime();
      stopWithHeadsign.setStopHeadsign(STOP_TEST_DIRECTION);
      Collection<StopTime> stopTimes = List.of(stopWithHeadsign, EMPTY_STOPPOINT, EMPTY_STOPPOINT);

      TripTimes tripTimes = new TripTimes(trip, stopTimes, new Deduplicator());

      I18NString headsignFirstStop = tripTimes.getHeadsign(0);
      assertEquals(STOP_TEST_DIRECTION, headsignFirstStop);

      I18NString headsignSecondStop = tripTimes.getHeadsign(1);
      assertEquals(DIRECTION, headsignSecondStop);
    }
  }

  @Test
  public void testStopUpdate() {
    TripTimes updatedTripTimesA = new TripTimes(createInitialTripTimes());

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
    TripTimes updatedTripTimesA = new TripTimes(createInitialTripTimes());

    updatedTripTimesA.updateDepartureTime(0, 30);

    assertEquals(30, updatedTripTimesA.getDepartureTime(0));
    assertEquals(60, updatedTripTimesA.getArrivalTime(1));
  }

  @Test
  public void testNegativeDwellTime() {
    TripTimes updatedTripTimesA = new TripTimes(createInitialTripTimes());

    updatedTripTimesA.updateArrivalTime(1, 60);
    updatedTripTimesA.updateDepartureTime(1, 59);

    var error = updatedTripTimesA.validateNonIncreasingTimes();
    assertTrue(error.isPresent());
    assertEquals(1, error.get().stopIndex());
    assertEquals(NEGATIVE_DWELL_TIME, error.get().code());
  }

  @Test
  public void testNegativeHopTime() {
    TripTimes updatedTripTimesB = new TripTimes(createInitialTripTimes());

    updatedTripTimesB.updateDepartureTime(6, 421);
    updatedTripTimesB.updateArrivalTime(7, 420);

    var error = updatedTripTimesB.validateNonIncreasingTimes();
    assertTrue(error.isPresent());
    assertEquals(7, error.get().stopIndex());
    assertEquals(NEGATIVE_HOP_TIME, error.get().code());
  }

  /**
   * Test negative hop time with stop cancellations.
   * Scheduled: 5 at 300, 6 at 360, 7 at 420
   * Test case: 5 at 421, 6 cancelled (with internal representation 481, since delays are propagated
   * to later stops without arrival or departure), 7 at 420
   * Result: Error to be present at stop 7, due to negative hop time
   */
  @Test
  public void testNegativeHopTimeWithStopCancellations() {
    TripTimes updatedTripTimesB = new TripTimes(createInitialTripTimes());

    updatedTripTimesB.updateDepartureTime(5, 421);
    updatedTripTimesB.updateArrivalTime(6, 481);
    updatedTripTimesB.updateDepartureTime(6, 481);
    updatedTripTimesB.setCancelled(6);
    updatedTripTimesB.updateArrivalTime(7, 420);

    var error = updatedTripTimesB.validateNonIncreasingTimes();
    assertTrue(error.isPresent());
    assertEquals(7, error.get().stopIndex());
    assertEquals(NEGATIVE_HOP_TIME, error.get().code());
  }

  /**
   * Test positive hop time with stop cancellations when buses run late.
   * Scheduled: 5 at 300, 6 at 360, 7 at 420
   * Test case: 5 at 400, 6 cancelled (with internal representation 460, since delays are propagated
   * to later stops without arrival or departure), 7 at 420.
   * Result: Expect no errors, since 6 is cancelled, and 5 is earlier than 7.
   */
  @Test
  public void testPositiveHopTimeWithStopCancellationsLate() {
    TripTimes updatedTripTimesB = new TripTimes(createInitialTripTimes());

    updatedTripTimesB.updateDepartureTime(5, 400);
    updatedTripTimesB.updateArrivalTime(6, 460);
    updatedTripTimesB.updateDepartureTime(6, 460);
    updatedTripTimesB.setCancelled(6);
    updatedTripTimesB.updateArrivalTime(7, 420);

    var error = updatedTripTimesB.validateNonIncreasingTimes();
    assertFalse(error.isPresent());
  }

  /**
   * Test positive hop time with stop cancellations when buses run early.
   * Scheduled: 5 at 300, 6 at 360, 7 at 420
   * Test case: 5 at 300, 6 cancelled(with internal representation 360, since delays are propagated
   * to later stops without arrival or departure), 7 at 320.
   * Result: Expect no errors, since 6 is cancelled, and 5 is still earlier than 7.
   */
  @Test
  public void testPositiveHopTimeWithStopCancellationsEarly() {
    TripTimes updatedTripTimesB = new TripTimes(createInitialTripTimes());

    updatedTripTimesB.updateDepartureTime(5, 300);
    updatedTripTimesB.setCancelled(6);
    updatedTripTimesB.updateArrivalTime(7, 320);

    var error = updatedTripTimesB.validateNonIncreasingTimes();
    assertFalse(error.isPresent());
  }

  /**
   * Test positive hop time with stop cancellations at the beginning of the trip.
   * Scheduled: 0 at 0, 1 at 60, 2 at 120, 3 at 180, 4 at 240, 5 at 300, 6 at 360, 7 at 420
   * Test case: 0 and 1 cancelled, start trip at stop 2 at time 0.
   * Result: Expect no errors, since 0 and 1 is cancelled, and 2 is still earlier than the others.
   */
  @Test
  public void testPositiveHopTimeWithTerminalCancellation() {
    TripTimes updatedTripTimesB = new TripTimes(createInitialTripTimes());

    updatedTripTimesB.setCancelled(0);
    updatedTripTimesB.setCancelled(1);
    updatedTripTimesB.updateArrivalTime(2, 0);
    updatedTripTimesB.updateDepartureTime(2, 10);

    var error = updatedTripTimesB.validateNonIncreasingTimes();
    assertFalse(error.isPresent());
  }

  /**
   * Test positive hop time with stop cancellations at the beginning of the trip.
   * Scheduled: 0 at 0, 1 at 60, 2 at 120, 3 at 180, 4 at 240, 5 at 300, 6 at 360, 7 at 420
   * Test case: 0 and 1 have no real-time data, trip arrived at stop 2 at time 30.
   * Result: Expect no errors, since 0 and 1 have no real-time data, and arrival at 2 can be earlier
   * than scheduled time at 1.
   */
  @Test
  public void testPositiveHopTimeWithTerminalNoData() {
    TripTimes updatedTripTimesB = new TripTimes(createInitialTripTimes());

    updatedTripTimesB.setNoData(0);
    updatedTripTimesB.setNoData(1);
    updatedTripTimesB.updateArrivalTime(2, 30);
    updatedTripTimesB.updateDepartureTime(2, 30);

    var error = updatedTripTimesB.validateNonIncreasingTimes();
    assertFalse(error.isPresent());
  }

  @Test
  public void testNonIncreasingUpdateCrossingMidnight() {
    TripTimes updatedTripTimesA = new TripTimes(createInitialTripTimes());

    updatedTripTimesA.updateArrivalTime(0, -300); //"Yesterday"
    updatedTripTimesA.updateDepartureTime(0, 50);

    assertTrue(updatedTripTimesA.validateNonIncreasingTimes().isEmpty());
  }

  @Test
  public void testDelay() {
    TripTimes updatedTripTimesA = new TripTimes(createInitialTripTimes());
    updatedTripTimesA.updateDepartureDelay(0, 10);
    updatedTripTimesA.updateArrivalDelay(6, 13);

    assertEquals(10, updatedTripTimesA.getDepartureTime(0));
    assertEquals(6 * 60 + 13, updatedTripTimesA.getArrivalTime(6));
  }

  @Test
  public void testCancel() {
    TripTimes updatedTripTimesA = new TripTimes(createInitialTripTimes());
    updatedTripTimesA.cancelTrip();
    assertEquals(RealTimeState.CANCELED, updatedTripTimesA.getRealTimeState());
  }

  @Test
  public void testNoData() {
    TripTimes updatedTripTimesA = new TripTimes(createInitialTripTimes());
    updatedTripTimesA.setNoData(1);
    assertFalse(updatedTripTimesA.isNoDataStop(0));
    assertTrue(updatedTripTimesA.isNoDataStop(1));
    assertFalse(updatedTripTimesA.isNoDataStop(2));
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
  public void testApply() {
    Trip trip = TransitModelForTest.trip(TRIP_ID).build();

    List<StopTime> stopTimes = new LinkedList<>();

    StopTime stopTime0 = new StopTime();
    StopTime stopTime1 = new StopTime();
    StopTime stopTime2 = new StopTime();

    RegularStop stop0 = TransitModelForTest.stopForTest(stops.get(0).getId(), 0.0, 0.0);
    RegularStop stop1 = TransitModelForTest.stopForTest(stops.get(1).getId(), 0.0, 0.0);
    RegularStop stop2 = TransitModelForTest.stopForTest(stops.get(2).getId(), 0.0, 0.0);

    stopTime0.setStop(stop0);
    stopTime0.setDepartureTime(0);
    stopTime0.setStopSequence(0);

    stopTime1.setStop(stop1);
    stopTime1.setArrivalTime(30);
    stopTime1.setDepartureTime(60);
    stopTime1.setStopSequence(1);

    stopTime2.setStop(stop2);
    stopTime2.setArrivalTime(90);
    stopTime2.setStopSequence(2);

    stopTimes.add(stopTime0);
    stopTimes.add(stopTime1);
    stopTimes.add(stopTime2);

    TripTimes differingTripTimes = new TripTimes(trip, stopTimes, new Deduplicator());

    TripTimes updatedTripTimesA = new TripTimes(differingTripTimes);

    updatedTripTimesA.updateArrivalTime(1, 89);
    updatedTripTimesA.updateDepartureTime(1, 98);

    var validationResult = updatedTripTimesA.validateNonIncreasingTimes();
    assertTrue(validationResult.isPresent());
    assertEquals(2, validationResult.get().stopIndex());
    assertEquals(NEGATIVE_DWELL_TIME, validationResult.get().code());
  }
}
