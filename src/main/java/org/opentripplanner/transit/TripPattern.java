package org.opentripplanner.transit;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;

/**
 * This is like a transmodel JourneyPattern.
 * All the trips on the same Route that have the same sequence of stops, with the same pickup/dropoff options.
 */
public class TripPattern implements Serializable {

    String routeId;
    int[] stops;
    // Could be compacted into 2 bits each or a bunch of flags, but is it even worth it?
    PickDropType[] pickups;
    PickDropType[] dropoffs;
    BitSet wheelchairAccessible;
    List<TripSchedule> tripSchedules = new ArrayList<>();

    public TripPattern (List<PickDropStop> pattern) {
        int nStops = pattern.size();
        stops = new int[nStops];
        pickups = new PickDropType[nStops];
        dropoffs = new PickDropType[nStops];
        wheelchairAccessible = new BitSet(nStops);
        int s = 0;
        for (PickDropStop pds : pattern) {
            stops[s] = pds.stop;
            pickups[s] = pds.pickupType;
            dropoffs[s] = pds.dropoffType;
        }
    }

    public void addTrip (TripSchedule tripSchedule) {
        tripSchedules.add(tripSchedule);
    }

    // Simply write "graph builder annotations" to a log file alongside the graphs.
    // function in gtfs-lib getOrderedStopTimes(string tripId)
    // Test GTFS loading on NL large data set.

    /**
     * Used as a map key when grouping trips by stop pattern.
     * These objects are not retained after the grouping process.
     */
    public static class Key {
        String routeId;
        int stop;
        PickDropType pickupType;
        PickDropType dropoffType;

        public static Key fromStopTimes(String routeId) {
            return null;
        }

    }

}
