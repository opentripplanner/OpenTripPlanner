package org.opentripplanner.routing.trippattern;

import org.junit.Test;
import org.opentripplanner.model.FeedScopedId;
import org.opentripplanner.model.Stop;
import org.opentripplanner.model.StopTime;
import org.opentripplanner.model.Trip;

import java.util.LinkedList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public class TripTimesTest {
    private static final FeedScopedId TRIP_ID = new FeedScopedId("agency", "testTripId");
    private static final FeedScopedId ROUTE_ID = new FeedScopedId("agency", "testRrouteId");

    private static final FeedScopedId STOP_A = new FeedScopedId("agency", "A"); // 0
    private static final FeedScopedId STOP_B = new FeedScopedId("agency", "B"); // 1
    private static final FeedScopedId STOP_C = new FeedScopedId("agency", "C"); // 2
    private static final FeedScopedId STOP_D = new FeedScopedId("agency", "D"); // 3
    private static final FeedScopedId STOP_E = new FeedScopedId("agency", "E"); // 4
    private static final FeedScopedId STOP_F = new FeedScopedId("agency", "F"); // 5
    private static final FeedScopedId STOP_G = new FeedScopedId("agency", "G"); // 6
    private static final FeedScopedId STOP_H = new FeedScopedId("agency", "H"); // 7

    private static final FeedScopedId[] stops =
        {STOP_A, STOP_B, STOP_C, STOP_D, STOP_E, STOP_F, STOP_G, STOP_H};

    private static final TripTimes originalTripTimes;

    static {
        Trip trip = new Trip(TRIP_ID);

        List<StopTime> stopTimes = new LinkedList<StopTime>();

        for(int i =  0; i < stops.length; ++i) {
            StopTime stopTime = new StopTime();

            Stop stop = Stop.stopForTest(stops[i].getId(), 0.0, 0.0);
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

        updatedTripTimesA.updateDepartureTime(0, TripTimes.UNAVAILABLE);

        assertEquals(TripTimes.UNAVAILABLE, updatedTripTimesA.getDepartureTime(0));
        assertEquals(60, updatedTripTimesA.getArrivalTime(1));
    }

    @Test
    public void testNonIncreasingUpdate() {
        TripTimes updatedTripTimesA = new TripTimes(originalTripTimes);

        updatedTripTimesA.updateArrivalTime(1, 60);
        updatedTripTimesA.updateDepartureTime(1, 59);

        assertFalse(updatedTripTimesA.timesIncreasing());

        TripTimes updatedTripTimesB = new TripTimes(originalTripTimes);

        updatedTripTimesB.updateDepartureTime(6, 421);
        updatedTripTimesB.updateArrivalTime(7, 420);

        assertFalse(updatedTripTimesB.timesIncreasing());
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
        updatedTripTimesA.cancel();

        for (int i = 0; i < stops.length - 1; i++) {
            assertEquals(originalTripTimes.getDepartureTime(i),
                    updatedTripTimesA.getScheduledDepartureTime(i));
            assertEquals(originalTripTimes.getArrivalTime(i),
                    updatedTripTimesA.getScheduledArrivalTime(i));
            assertEquals(TripTimes.UNAVAILABLE, updatedTripTimesA.getDepartureTime(i));
            assertEquals(TripTimes.UNAVAILABLE, updatedTripTimesA.getArrivalTime(i));
        }
    }

    @Test
    public void testApply() {
        Trip trip = new Trip(TRIP_ID);

        List<StopTime> stopTimes = new LinkedList<>();

        StopTime stopTime0 = new StopTime();
        StopTime stopTime1 = new StopTime();
        StopTime stopTime2 = new StopTime();

        Stop stop0 = Stop.stopForTest(stops[0].getId(), 0.0, 0.0);
        Stop stop1 = Stop.stopForTest(stops[1].getId(), 0.0, 0.0);
        Stop stop2 = Stop.stopForTest(stops[2].getId(), 0.0, 0.0);

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

        assertFalse(updatedTripTimesA.timesIncreasing());
    }

    @Test
    public void testGetRunningTime() {
        for (int i = 0; i < stops.length - 1; i++) {
            assertEquals(60, originalTripTimes.getRunningTime(i));
        }

        TripTimes updatedTripTimes = new TripTimes(originalTripTimes);

        for (int i = 0; i < stops.length - 1; i++) {
            updatedTripTimes.updateDepartureDelay(i, i);
        }

        for (int i = 0; i < stops.length - 1; i++) {
            assertEquals(60 - i, updatedTripTimes.getRunningTime(i));
        }
    }

    @Test
    public void testGetDwellTime() {
        for (int i = 0; i < stops.length; i++) {
            assertEquals(0, originalTripTimes.getDwellTime(i));
        }

        TripTimes updatedTripTimes = new TripTimes(originalTripTimes);

        for (int i = 0; i < stops.length; i++) {
            updatedTripTimes.updateArrivalDelay(i, -i);
        }

        for (int i = 0; i < stops.length; i++) {
            assertEquals(i, updatedTripTimes.getDwellTime(i));
        }
    }
}
