package org.opentripplanner.routing.trippattern;

import java.io.Serializable;
import org.opentripplanner.model.Frequency;

/**
 * Uses a TripTimes to represent multiple trips following the same template at regular intervals.
 * (see GTFS frequencies.txt)
 */
public class FrequencyEntry implements Serializable {

    private static final long serialVersionUID = 1L;

    public final int startTime; // sec after midnight
    public final int endTime;   // sec after midnight
    public final int headway;   // sec
    public final boolean exactTimes;
    public final TripTimes tripTimes;

    public FrequencyEntry(Frequency freq, TripTimes tripTimes) {
        this.startTime  = freq.getStartTime();
        this.endTime    = freq.getEndTime();
        this.headway    = freq.getHeadwaySecs();
        this.exactTimes = freq.getExactTimes() != 0;
        this.tripTimes  = tripTimes;
    }

    public FrequencyEntry (int startTime, int endTime, int headway, boolean exactTimes, TripTimes tripTimes) {
        this.startTime = startTime;
        this.endTime = endTime;
        this.headway = headway;
        this.exactTimes = exactTimes;
        this.tripTimes = tripTimes;

    }

    /*
        The TripTimes getDepartureTime / getArrivalTime methods do not care when the search is happening.
        The Frequency equivalents need to know when the search is happening, and need to be able to say
        no trip is possible. Therefore we need to add another specialized method.

        Fortunately all uses of the TripTimes itself in traversing edges use relative times,
        so we can fall back on the underlying TripTimes.
     */

    @Override
    public String toString() {
        return String.format("FreqEntry: trip %s start %s end %s headway %s",
            tripTimes.getTrip(), formatSeconds(startTime), formatSeconds(endTime), formatSeconds(headway));
    }

    public int nextDepartureTime (int stop, int time) {
        // Start time and end time are for the first stop in the trip. Find the time offset for this stop.
        int stopOffset = tripTimes.getDepartureTime(stop) - tripTimes.getDepartureTime(0);
        int beg = startTime + stopOffset; // First time a vehicle passes by this stop.
        int end = endTime + stopOffset; // Latest a vehicle can pass by this stop.
        if (time > end) { return -1; }
        if (exactTimes) {
            for (int dep = beg; dep < end; dep += headway) {
                if (dep >= time) { return dep; }
            }
        } else {
            int dep = time + headway;
            // TODO it might work better to step forward until in range
            // this would work better for time window edges.
            if (dep < beg) { return beg; } // not quite right
            if (dep < end) { return dep; }
        }
        return -1;
    }

    public int prevArrivalTime (int stop, int t) {
        int stopOffset = tripTimes.getArrivalTime(stop) - tripTimes.getDepartureTime(0);
        int beg = startTime + stopOffset; // First time a vehicle passes by this stop.
        int end = endTime + stopOffset; // Latest a vehicle can pass by this stop.
        if(t < beg) return -1;
        if (exactTimes) {
            // we can't start from end in case end - beg is not a multiple of headway
            int arr;
            for (arr = beg + headway; arr < end; arr += headway) {
                if (arr > t) { return arr - headway; }
            }
            // if t > end, return last valid arrival time
            return arr - headway;
        } else {
            int dep = t - headway;
            if (dep > end) { return end; } // not quite right
            if (dep > beg) { return dep; }
        }
        return -1;
    }

    /**
     * Returns a disposable TripTimes for this frequency entry in which the vehicle
     * passes the given stop index (not stop sequence number) at the given time.
     * This allows us to separate the departure/arrival search process from
     * actually instantiating a TripTimes, to avoid making too many short-lived clones.
     * This delegation is a sign that maybe FrequencyEntry should subclass TripTimes.
     */
    public TripTimes materialize (int stop, int time, boolean depart) {
        return tripTimes.timeShift(stop, time, depart);
    }

    /** @return the maximum number of trips this frequency entry could represent, given its headway. */
    public int numTrips() {
        return (endTime - startTime) / headway;
    }

    /** @return the minimum time in seconds since midnight at which a trip may depart on this frequency definition. */
    public int getMinDeparture() {
        // this is simple: the earliest this trip could depart is the time at which it starts plus the dwell at the first stop
        return tripTimes.getDepartureTime(0) - tripTimes.getArrivalTime(0) + startTime;
    }

    /** @return the maximum time in seconds since midnight at which a trip may arrive on this frequency definition. */
    public int getMaxArrival() {
        // The latest this trip could arrive is its last arrival time minus its first arrival time (the length of the trip),
        // plus the end time (the latest it could have arrived at the initial stop)
        return tripTimes.getArrivalTime(tripTimes.getNumStops() - 1) - tripTimes.getArrivalTime(0) + endTime;
    }

    /** Used in debugging / dumping times. */
    private static String formatSeconds(int s) {
        int m = s / 60;
        s = s % 60;
        final int h = m / 60;
        m = m % 60;
        return String.format("%02d:%02d:%02d", h, m, s);
    }
}
