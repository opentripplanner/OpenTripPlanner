package org.opentripplanner.model;

import org.junit.Test;

import java.util.Collections;

import static org.junit.Assert.assertEquals;

public class TripStopTimesTest {

    @Test
    public void reindex() {
        // Given a stop
        Stop stop = new Stop();
        stop.setId(new FeedScopedId("1", "S1"));

        // And a trip
        Trip trip = new Trip();
        trip.setId(new FeedScopedId("1", "T1"));

        // And a StopTime for that trip and stop
        StopTime stopTime = new StopTime();
        stopTime.setTrip(trip);
        stopTime.setStop(stop);


        // And a map of stopTimes by trip (TripStopTimes)
        TripStopTimes stopTimesByTrip = new TripStopTimes();
        stopTimesByTrip.addAll(Collections.singletonList(stopTime));

        // Then verify the map contains a list of stop times for that trip
        assertEquals(1, stopTimesByTrip.get(trip).size());


        // Then the trip id changes
        trip.setId(new FeedScopedId("A", "T1"));


        // And the internal map index in `stopTimesByTrip` is invalid;
        // Hence trying to get the stop times fails, because the #hashCode
        // now generate a new value
        assertEquals(0, stopTimesByTrip.get(trip).size());


        // To fix the problem reindex the map
        stopTimesByTrip.reindex();

        // And verify that the stop times exist for the given trip
        assertEquals(1, stopTimesByTrip.get(trip).size());
    }
}