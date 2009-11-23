package org.opentripplanner.routing.edgetype;

import java.io.Serializable;
import java.lang.reflect.Array;
import java.util.Collections;
import java.util.List;
import java.util.Vector;

import org.onebusaway.gtfs.model.StopTime;
import org.onebusaway.gtfs.model.Trip;
import org.opentripplanner.routing.edgetype.factory.TripOvertakingException;

public final class TripPattern implements Serializable {

    private static final long serialVersionUID = 1L;

    /*
     * Represents a class of trips distinguished by service id, list of stops, dwell time at stops,
     * and running time between stops.
     */

    public Trip exemplar;

    private Vector<Integer>[] departureTimes;

    private Vector<Integer>[] runningTimes;

    class PatternStop {
        public int arrivalTimeOffset;

        public int departureTimeOffset;

        public PatternStop(int arrivalTimeOffset, int departureTimeOffset) {
            this.arrivalTimeOffset = arrivalTimeOffset;
            this.departureTimeOffset = departureTimeOffset;
        }
    }

    @SuppressWarnings("unchecked")
    public TripPattern(Trip exemplar, List<StopTime> stopTimes) {
        this.exemplar = exemplar;
        departureTimes = (Vector<Integer>[]) Array.newInstance(Vector.class, stopTimes.size());
        runningTimes = (Vector<Integer>[]) Array.newInstance(Vector.class, stopTimes.size());

        for (int i = 0; i < stopTimes.size(); ++i) {
            departureTimes[i] = new Vector<Integer>();
            runningTimes[i] = new Vector<Integer>();

        }
    }

    public void removeHop(int stopindex, int hop) {

    }

    public void addHop(int stopIndex, int insertionPoint, int departureTime, int runningTime) {
        Vector<Integer> stopRunningTimes = runningTimes[stopIndex];
        Vector<Integer> stopDepartureTimes = departureTimes[stopIndex];

        // throw an exception when this departure time is not between the departure times it
        // should be between, indicating a trip that overtakes another.

        if (insertionPoint > 0) {
            if (stopDepartureTimes.elementAt(insertionPoint - 1) > departureTime) {
                throw new TripOvertakingException();
            }
        }
        if (insertionPoint < stopDepartureTimes.size()) {
            if (stopDepartureTimes.elementAt(insertionPoint) < departureTime) {
                throw new TripOvertakingException();
            }
        }
        stopDepartureTimes.insertElementAt(departureTime, insertionPoint);
        stopRunningTimes.insertElementAt(runningTime, insertionPoint);
    }

    public int getNextPattern(int stopIndex, int afterTime) {
        Vector<Integer> stopDepartureTimes = departureTimes[stopIndex];
        int index = Collections.binarySearch(stopDepartureTimes, afterTime);
        if (index == -stopDepartureTimes.size() - 1)
            return -1;

        if (index >= 0) {
            return index;
        } else {
            return -index - 1;
        }
    }

    public int getRunningTime(int stopIndex, int pattern) {
        Vector<Integer> stopRunningTimes = runningTimes[stopIndex];
        return stopRunningTimes.get(pattern);
    }

    public int getDepartureTime(int stopIndex, int pattern) {
        Vector<Integer> stopDepartureTimes = departureTimes[stopIndex];
        return stopDepartureTimes.get(pattern);
    }

    public int getInsertionPoint(int departureTime) {
        Vector<Integer> stopDepartureTimes = departureTimes[0];
        int index = Collections.binarySearch(stopDepartureTimes, departureTime);
        return -index - 1;
    }
}
