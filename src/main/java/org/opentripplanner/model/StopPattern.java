package org.opentripplanner.model;

import com.google.common.hash.HashCode;
import com.google.common.hash.HashFunction;
import com.google.common.hash.Hasher;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;

/**
 * This class represents what is called a JourneyPattern in Transmodel: the sequence of stops at
 * which a trip (GTFS) or vehicle journey (Transmodel) calls, irrespective of the day on which 
 * service runs.
 * 
 * An important detail: Routes in GTFS are not a structurally important element, they just serve as
 * user-facing information. It is possible for the same journey pattern to appear in more than one
 * route. 
 * 
 * OTP already has several classes that represent this same thing: A TripPattern in the context of
 * routing. It represents all trips with the same stop pattern A ScheduledStopPattern in the GTFS
 * loading process. A RouteVariant in the TransitIndex, which has a unique human-readable name and
 * belongs to a particular route.
 * 
 * We would like to combine all these different classes into one.
 * 
 * Any two trips with the same stops in the same order, and that operate on the same days, can be
 * combined using a TripPattern to simplify the graph. This saves memory and reduces search
 * complexity since we only consider the trip that departs soonest for each pattern. Field
 * calendarId has been removed. See issue #1320.
 * 
 * A StopPattern is very closely related to a TripPattern -- it essentially serves as the unique
 * key for a TripPattern. Should the route be included in the StopPattern?
 */
public class StopPattern implements Serializable {

    private static final long serialVersionUID = 20140101L;
    
    private final Stop[] stops;
    private final PickDrop[]  pickups;
    private final PickDrop[]  dropoffs;

    private StopPattern (int size) {
        stops     = new Stop[size];
        pickups   = new PickDrop[size];
        dropoffs  = new PickDrop[size];
    }

    /** Assumes that stopTimes are already sorted by time. */
    public StopPattern (Collection<StopTime> stopTimes) {
        this (stopTimes.size());
        int size = stopTimes.size();
        if (size == 0) return;
        Iterator<StopTime> stopTimeIterator = stopTimes.iterator();

        for (int i = 0; i < size; ++i) {
            StopTime stopTime = stopTimeIterator.next();
            stops[i] = (Stop) stopTime.getStop();
            // should these just be booleans? anything but 1 means pick/drop is allowed.
            // pick/drop messages could be stored in individual trips
            pickups[i] = stopTime.getPickupType();
            dropoffs[i] = stopTime.getDropOffType();
        }
        /*
         * TriMet GTFS has many trips that differ only in the pick/drop status of their initial and
         * final stops. This may have something to do with interlining. They are turning pickups off
         * on the final stop of a trip to indicate that there is no interlining, because they supply
         * block IDs for all trips, even those followed by dead runs. See issue 681. Enabling
         * dropoffs at the initial stop and pickups at the final merges similar patterns while
         * having no effect on routing.
         */
        dropoffs[0] = PickDrop.SCHEDULED;
        pickups[size - 1] = PickDrop.SCHEDULED;
    }

    /**
     * @param stopId in agency_id format
     */
    public boolean containsStop (String stopId) {
        if (stopId == null) { return false; }
        for (Stop stop : stops) if (stopId.equals(stop.getId().toString())) { return true; }
        return false;
    }

    public int getSize() {
        return stops.length;
    }

    public boolean equals(Object other) {
        if (other instanceof StopPattern) {
            StopPattern that = (StopPattern) other;
            return Arrays.equals(this.stops, that.stops) &&
                   Arrays.equals(this.pickups, that.pickups) &&
                   Arrays.equals(this.dropoffs, that.dropoffs);
        } else {
            return false;
        }
    }

    public int hashCode() {
        int hash = stops.length;
        hash += Arrays.hashCode(this.stops);
        hash *= 31;
        hash += Arrays.hashCode(this.pickups);
        hash *= 31;
        hash += Arrays.hashCode(this.dropoffs);
        return hash;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("StopPattern: ");
        for (int i = 0, j = stops.length; i < j; ++i) {
            sb.append(String.format("%s_%s%s ", stops[i].getCode(), pickups[i], dropoffs[i]));
        }
        return sb.toString();
    }

    /**
     * In most cases we want to use identity equality for StopPatterns. There is a single
     * StopPattern instance for each semantic StopPattern, and we don't want to calculate
     * complicated hashes or equality values during normal execution. However, in some cases we
     * want a way to consistently identify trips across versions of a GTFS feed, when the feed
     * publisher cannot ensure stable trip IDs. Therefore we define some additional hash functions.
     */
    public HashCode semanticHash(HashFunction hashFunction) {
        Hasher hasher = hashFunction.newHasher();
        int size = stops.length;
        for (int s = 0; s < size; s++) {
            Stop stop = stops[s];
            // Truncate the lat and lon to 6 decimal places in case they move slightly between
            // feed versions
            hasher.putLong((long) (stop.getLat() * 1000000));
            hasher.putLong((long) (stop.getLon() * 1000000));
        }
        // Use hops rather than stops because drop-off at stop 0 and pick-up at last stop are
        // not important and have changed between OTP versions.
        for (int hop = 0; hop < size - 1; hop++) {
            hasher.putInt(pickups[hop].getGtfsCode());
            hasher.putInt(dropoffs[hop + 1].getGtfsCode());
        }
        return hasher.hash();
    }

    public Stop[] getStops() {
        return stops;
    }

    public Stop getStop(int i) {
        return stops[i];
    }

    public PickDrop getPickup(int i) {
        return pickups[i];
    }

    public PickDrop getDropoff(int i) {
        return dropoffs[i];
    }
}
