package org.opentripplanner.routing.trippattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opentripplanner.model.UpdateError.UpdateErrorType.NEGATIVE_DWELL_TIME;
import static org.opentripplanner.model.UpdateError.UpdateErrorType.NEGATIVE_HOP_TIME;

import java.util.LinkedList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.opentripplanner.model.StopTime;
import org.opentripplanner.transit.model._data.TransitModelForTest;
import org.opentripplanner.transit.model.framework.Deduplicator;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.transit.model.site.RegularStop;
import org.opentripplanner.transit.model.timetable.RealTimeState;
import org.opentripplanner.transit.model.timetable.Trip;
import org.opentripplanner.transit.model.timetable.TripTimes;

public class TripTimesTest {

  private static final String TRIP_ID = "testTripId";

  private static final FeedScopedId STOP_A = TransitModelForTest.id("A"); // 0
  private static final FeedScopedId STOP_B = TransitModelForTest.id("B"); // 1
  private static final FeedScopedId STOP_C = TransitModelForTest.id("C"); // 2
  private static final FeedScopedId STOP_D = TransitModelForTest.id("D"); // 3
  private static final FeedScopedId STOP_E = TransitModelForTest.id("E"); // 4
  private static final FeedScopedId STOP_F = TransitModelForTest.id("F"); // 5
  private static final FeedScopedId STOP_G = TransitModelForTest.id("G"); // 6
  private static final FeedScopedId STOP_H = TransitModelForTest.id("H"); // 7

  private static final FeedScopedId[] stops = {
    STOP_A,
    STOP_B,
    STOP_C,
    STOP_D,
    STOP_E,
    STOP_F,
    STOP_G,
    STOP_H,
  };

  private static final TripTimes originalTripTimes;

  static {
    Trip trip = TransitModelForTest.trip(TRIP_ID).build();

    List<StopTime> stopTimes = new LinkedList<>();

    for (int i = 0; i < stops.length; ++i) {
      StopTime stopTime = new StopTime();

      RegularStop stop = TransitModelForTest.stopForTest(stops[i].getId(), 0.0, 0.0);
      stopTime.setStop(stop);
      stopTime.setArrivalTime(i * 60);
      stopTime.setDepartureTime(i * 60);
      stopTime.setStopSequence(i);
      stopTimes.add(stopTime);
    }

    originalTripTimes = new TripTimes(trip, stopTimes, new Deduplicator());
  }

  @Test
  public void testStopUpdate() {
    TripTimes updatedTripTimesA = new TripTimes(originalTripTimes);

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
    TripTimes updatedTripTimesA = new TripTimes(originalTripTimes);

    updatedTripTimesA.updateDepartureTime(0, 30);

    assertEquals(30, updatedTripTimesA.getDepartureTime(0));
    assertEquals(60, updatedTripTimesA.getArrivalTime(1));
  }

  @Test
  public void testNonIncreasingUpdate() {
    TripTimes updatedTripTimesA = new TripTimes(originalTripTimes);

    updatedTripTimesA.updateArrivalTime(1, 60);
    updatedTripTimesA.updateDepartureTime(1, 59);

    var error = updatedTripTimesA.validateNonIncreasingTimes();
    assertTrue(error.isFailure());
    assertEquals(1, error.failureValue().stopIndex());
    assertEquals(NEGATIVE_DWELL_TIME, error.failureValue().errorType());

    TripTimes updatedTripTimesB = new TripTimes(originalTripTimes);

    updatedTripTimesB.updateDepartureTime(6, 421);
    updatedTripTimesB.updateArrivalTime(7, 420);

    error = updatedTripTimesB.validateNonIncreasingTimes();
    assertTrue(error.isFailure());
    assertEquals(7, error.failureValue().stopIndex());
    assertEquals(NEGATIVE_HOP_TIME, error.failureValue().errorType());
  }

  @Test
  public void testNonIncreasingUpdateCrossingMidnight() {
    TripTimes updatedTripTimesA = new TripTimes(originalTripTimes);

    updatedTripTimesA.updateArrivalTime(0, -300); //"Yesterday"
    updatedTripTimesA.updateDepartureTime(0, 50);

    assertTrue(updatedTripTimesA.validateNonIncreasingTimes().isSuccess());
  }

  @Test
  public void testDelay() {
    TripTimes updatedTripTimesA = new TripTimes(originalTripTimes);
    updatedTripTimesA.updateDepartureDelay(0, 10);
    updatedTripTimesA.updateArrivalDelay(6, 13);

    assertEquals(10, updatedTripTimesA.getDepartureTime(0));
    assertEquals(6 * 60 + 13, updatedTripTimesA.getArrivalTime(6));
  }

  @Test
  public void testCancel() {
    TripTimes updatedTripTimesA = new TripTimes(originalTripTimes);
    updatedTripTimesA.cancelTrip();
    assertEquals(RealTimeState.CANCELED, updatedTripTimesA.getRealTimeState());
  }

  @Test
  public void testNoData() {
    TripTimes updatedTripTimesA = new TripTimes(originalTripTimes);
    updatedTripTimesA.setNoData(1);
    assertFalse(updatedTripTimesA.isNoDataStop(0));
    assertTrue(updatedTripTimesA.isNoDataStop(1));
    assertFalse(updatedTripTimesA.isNoDataStop(2));
  }

  @Test
  public void testApply() {
    Trip trip = TransitModelForTest.trip(TRIP_ID).build();

    List<StopTime> stopTimes = new LinkedList<>();

    StopTime stopTime0 = new StopTime();
    StopTime stopTime1 = new StopTime();
    StopTime stopTime2 = new StopTime();

    RegularStop stop0 = TransitModelForTest.stopForTest(stops[0].getId(), 0.0, 0.0);
    RegularStop stop1 = TransitModelForTest.stopForTest(stops[1].getId(), 0.0, 0.0);
    RegularStop stop2 = TransitModelForTest.stopForTest(stops[2].getId(), 0.0, 0.0);

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
    assertTrue(validationResult.isFailure());
    assertEquals(2, validationResult.failureValue().stopIndex());
    assertEquals(NEGATIVE_DWELL_TIME, validationResult.failureValue().errorType());
  }
}
