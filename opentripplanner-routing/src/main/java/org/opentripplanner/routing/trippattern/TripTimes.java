/* This program is free software: you can redistribute it and/or
 modify it under the terms of the GNU Lesser General Public License
 as published by the Free Software Foundation, either version 3 of
 the License, or (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>. */

package org.opentripplanner.routing.trippattern;

import java.util.Comparator;

import lombok.AllArgsConstructor;

import org.onebusaway.gtfs.model.Trip;
import org.opentripplanner.routing.core.RoutingRequest;
import org.opentripplanner.routing.request.BannedStopSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A TripTimes represents the arrival and departure times for a single trip in an Timetable. It
 * is carried along by States when routing to ensure that they have a consistent, fast view of the 
 * trip when realtime updates are being applied. 
 * All times are expressed as seconds since midnight (as in GTFS). The indexes into a StopTimes are 
 * not stop indexes, but inter-stop segment ("hop") indexes, so hop 0 refers to the hop between 
 * stops 0 and 1, and arrival 0 is actually an arrival at stop 1. The main reason for this is that 
 * it saves two extra array elements in every stopTimes. It might be worth it to just use stop 
 * indexes everywhere for simplicity.
 */
public abstract class TripTimes {

    private static final Logger LOG = LoggerFactory.getLogger(TripTimes.class);
    public static final int PASSED = -1;
    public static final int CANCELED = -2;
    
    /* ABSTRACT INSTANCE METHODS */
    
    /** @return the trips whose arrivals and departures are represented by this TripTimes */
    public abstract Trip getTrip();
    
//    /** @return the index of this TripTimes in the enclosing Timetable */
//      so far doesn't need to be visible
//    public Trip getTripIndex();

    /** @return the base trip times which this particular TripTimes represents or modifies */
    public abstract ScheduledTripTimes getScheduledTripTimes();

    /** @return the number of inter-stop segments (hops) on this trip */
    public abstract int getNumHops();
        
    /** 
     * @return the time in seconds after midnight at which the vehicle begins traversing each 
     * inter-stop segment ("hop"). 
     */
    public abstract int getDepartureTime(int hop);
    
    /** 
     * @return the time in seconds after midnight at which the vehicle arrives at the end of each 
     * inter-stop segment ("hop"). A null value indicates that all dwells are 0-length, and arrival 
     * times are to be derived from the departure times array. 
     */
    public abstract int getArrivalTime(int hop);

    /**
     * It all depends whether we store pointers to the enclosing Timetable in ScheduledTripTimes...
     */
    public abstract String getHeadsign(int hop);
        
    /* IMPLEMENTED INSTANCE METHODS */
    
    /** 
     * @return the amount of time in seconds that the vehicle waits at the stop *before* traversing 
     * each inter-stop segment ("hop"). It is undefined for hop 0, and at the end of a trip. 
     */
    public int getDwellTime(int hop) {
        // TODO: Add range checking and -1 error value? see GTFSPatternHopFactory.makeTripPattern().
        int arrivalTime = getArrivalTime(hop-1);
        int departureTime = getDepartureTime(hop);
        return departureTime - arrivalTime;
    }
    
    /** 
     * @return the length of time time in seconds that it takes for the vehicle to traverse each 
     * inter-stop segment ("hop"). 
     */
    public int getRunningTime(int hop) {
        return getArrivalTime(hop) - getDepartureTime(hop);
    }

    /** @return the difference between the scheduled and actual departure times for this hop. */
    public int getDepartureDelay(int hop) {
        return getDepartureTime(hop) - getScheduledTripTimes().getDepartureTime(hop); 
    }

    /** @return the difference between the scheduled and actual arrival times for this hop. */
    public int getArrivalDelay(int hop) {
        return getArrivalTime(hop) - getScheduledTripTimes().getArrivalTime(hop); 
    }
    
    /** 
     * @return true if this TripTimes represents an unmodified, scheduled trip from a published 
     * timetable or false if it is a updated, cancelled, or otherwise modified one.
     */
    public boolean isScheduled() {
        return this.getScheduledTripTimes() == this;
    }
    
    private String formatSeconds(int s) {
        int m = s / 60;
        s = s % 60;
        int h = m / 60;
        m = m % 60;
        return String.format("%02d:%02d:%02d", h, m, s);
    }

    /** Builds a string concisely representing all departure and arrival times in this TripTimes. */
    public String dumpTimes() {
        StringBuilder sb = new StringBuilder();
        int nHops = this.getNumHops();
        // compaction is multi-layered now
        //sb.append(arrivalTimes == null ? "C " : "U ");
        for (int hop=0; hop < nHops; hop++) {
            String s = String.format("(%d)%8s__%8s", hop, formatSeconds(this.getDepartureTime(hop)), 
                    formatSeconds(this.getArrivalTime(hop)));
            sb.append(s);
        }
        return sb.toString();
    }

    /** 
     * Request that this TripTimes be analyzed and its memory usage reduced if possible. Default
     * implementation does nothing since only certain TripTimes subclasses will need this.
     * @return whether or not compaction occurred. 
     */
    public boolean compact() {
        return false;
    }

    /**
     * When creating a scheduled TripTimes or wrapping it in updates, we could potentially imply
     * negative hop or dwell times. We really don't want those being used in routing. 
     * This method check that all times are increasing, and issues warnings if this is not the case.
     * @return whether the times were found to be increasing.
     */
    public boolean timesIncreasing() {
        // iterate over the new tripTimes, checking that dwells and hops are positive
        boolean increasing = true;
        int nHops = getNumHops();
        int prevArr = -1;
        for (int hop = 0; hop < nHops; hop++) {
            int dep = getDepartureTime(hop);
            int arr = getArrivalTime(hop);
            if (arr < dep) { // negative hop time
                LOG.error("Negative hop time in TripTimes at index {}.", hop);
                increasing = false;
            }
            if (prevArr > dep) { // negative dwell time before this hop
                LOG.error("Negative dwell time in TripTimes at index {}.", hop);
                increasing = false;
            }
            prevArr = arr;
        }
        return increasing;
    }
    
    /* STATIC METHODS TAKING TRIPTIMES AS ARGUMENTS */

    /**
     * Binary search method adapted from GNU Classpath Arrays.java (GPL). 
     * Range parameters and range checking removed.
     * Search across an array of TripTimes, looking only at a specific hop number.
     * 
     * @return the index at which the key was found, or the index of the first value higher than 
     * key if it was not found, or a.length if there is no such value. Note that this has been
     * changed from Arrays.binarysearch.
     */
    public static int binarySearchDepartures(TripTimes[] a, int hop, int key) {
        int low = 0;
        int hi = a.length - 1;
        int mid = 0;
        while (low <= hi) {
            mid = (low + hi) >>> 1;
            final int d = a[mid].getDepartureTime(hop);
            if (d == key)
                return mid;
            else if (d > key)
                hi = mid - 1;
            else
                // This gets the insertion point right on the last loop.
                low = ++mid;
        }
        return mid;
    }

    /**
     * Binary search method adapted from GNU Classpath Arrays.java (GPL). 
     * Range parameters and range checking removed.
     * Search across an array of TripTimes, looking only at a specific hop number.
     * 
     * @return the index at which the key was found, or the index of the first value *lower* than
     * key if it was not found, or -1 if there is no such value. Note that this has been changed
     * from Arrays.binarysearch: this is a mirror-image of the departure search algorithm.
     * 
     * TODO: I have worked through corner cases but should reverify with some critical distance.
     */
    public static int binarySearchArrivals(TripTimes[] a, int hop, int key) {
        int low = 0;
        int hi = a.length - 1;
        int mid = hi;
        while (low <= hi) {
            mid = (low + hi) >>> 1;
            final int d = a[mid].getArrivalTime(hop);
            if (d == key)
                return mid;
            else if (d < key)
                low = mid + 1;
            else
                // This gets the insertion point right on the last loop.
                hi = --mid;
        }
        return mid;
    }

    /**
     * Once a trip has been found departing or arriving at an appropriate time, check whether that 
     * trip fits other restrictive search criteria such as bicycle and wheelchair accessibility.
     * 
     * GTFS bike extensions based on mailing list message at: 
     * https://groups.google.com/d/msg/gtfs-changes/QqaGOuNmG7o/xyqORy-T4y0J
     * 2: bikes allowed
     * 1: no bikes allowed
     * 0: no information (same as field omitted)
     * 
     * If route OR trip explicitly allows bikes, bikes are allowed.
     * @param stopIndex 
     */
    public boolean tripAcceptable(RoutingRequest options, boolean bicycle, int stopIndex) {
        Trip trip = this.getTrip();
        BannedStopSet banned = options.bannedTrips.get(trip.getId());
        if (banned != null) {
            if (banned.contains(stopIndex) || banned == BannedStopSet.ALL) {
                return false;
            }
        }
        if (options.wheelchairAccessible && trip.getWheelchairAccessible() != 1)
            return false;
        if (bicycle)
            if ((trip.getTripBikesAllowed() != 2) &&    // trip does not explicitly allow bikes and
                (trip.getRoute().getBikesAllowed() != 2 // route does not explicitly allow bikes or  
                || trip.getTripBikesAllowed() == 1))    // trip explicitly forbids bikes
                return false; 
        return true;
    }


    /* NESTED STATIC CLASSES */
    
    /** Used for sorting an array of StopTimes based on arrivals for a specific hop. */
    @AllArgsConstructor
    public static class ArrivalsComparator implements Comparator<TripTimes> {
        final int hop; 
        @Override public int compare(TripTimes tt1, TripTimes tt2) {
            return tt1.getArrivalTime(hop) - tt2.getArrivalTime(hop);
        }
    }
        
    /** Used for sorting an array of StopTimes based on departures for a specific hop. */
    @AllArgsConstructor
    public static class DeparturesComparator implements Comparator<TripTimes> {
        final int hop; 
        @Override public int compare(TripTimes tt1, TripTimes tt2) {
            return tt1.getDepartureTime(hop) - tt2.getDepartureTime(hop);
        }
    }

}
