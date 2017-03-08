package org.opentripplanner.model;

import java.io.Serializable;
import java.util.Arrays;
import java.util.List;

import com.google.common.hash.HashCode;
import com.google.common.hash.HashFunction;
import com.google.common.hash.Hasher;
import com.google.common.hash.Hashing;
import org.onebusaway.gtfs.model.Stop;
import org.onebusaway.gtfs.model.StopTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
 * complexity since we only consider the trip that departs soonest for each pattern. AgencyAndId
 * calendarId has been removed. See issue #1320.
 * 
 * A StopPattern is very closely related to a TripPattern -- it essentially serves as the unique key for a TripPattern.
 * Should the route be included in the StopPattern?
 */
public class StopPattern implements Serializable {

    private static final long serialVersionUID = 20140101L;
    
    /* Constants for the GTFS pick up / drop off type fields. */
    // It would be nice to have an enum for these, but the equivalence with integers is important.
    public static final int PICKDROP_SCHEDULED = 0;
    public static final int PICKDROP_NONE = 1;
    public static final int PICKDROP_CALL_AGENCY = 2;
    public static final int PICKDROP_COORDINATE_WITH_DRIVER = 3;
    
    public final int size; // property could be derived from arrays
    public final Stop[] stops;
    public final int[]  pickups;
    public final int[]  dropoffs;
    public final int[] continuousPickups;
    public final int[] continuousDropoffs;

    public boolean equals(Object other) {
        if (other instanceof StopPattern) {
            StopPattern that = (StopPattern) other;
            return Arrays.equals(this.stops,    that.stops) && 
                   Arrays.equals(this.pickups,  that.pickups) && 
                   Arrays.equals(this.dropoffs, that.dropoffs) &&
                   Arrays.equals(this.continuousPickups, that.continuousPickups) &&
                   Arrays.equals(this.continuousDropoffs, that.continuousDropoffs);
        } else {
            return false;
        }
    }

    public int hashCode() {
        int hash = size;
        hash += Arrays.hashCode(this.stops);
        hash *= 31;
        hash += Arrays.hashCode(this.pickups);
        hash *= 31;
        hash += Arrays.hashCode(this.dropoffs);
        hash *= 31;
        hash += Arrays.hashCode(this.continuousPickups);
        hash *= 31;
        hash += Arrays.hashCode(this.continuousDropoffs);
        return hash;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("StopPattern: ");
        for (int i = 0, j = stops.length; i < j; ++i) {
            sb.append(String.format("%s_%d%d ", stops[i].getCode(), pickups[i], dropoffs[i]));
        }
        return sb.toString();
    }

    private StopPattern (int size) {
        this.size = size;
        stops     = new Stop[size];
        pickups   = new int[size];
        dropoffs  = new int[size];
        continuousPickups = new int[size];
        continuousDropoffs = new int[size];
    }

    /** Assumes that stopTimes are already sorted by time. */
    public StopPattern (List<StopTime> stopTimes) {
        this (stopTimes.size());
        if (size == 0) return;
        for (int i = 0; i < size; ++i) {
            StopTime stopTime = stopTimes.get(i);
            stops[i] = stopTime.getStop();
            // should these just be booleans? anything but 1 means pick/drop is allowed.
            // pick/drop messages could be stored in individual trips
            pickups[i] = stopTime.getPickupType();
            dropoffs[i] = stopTime.getDropOffType();

            // continuous pickup/dropoff can be empty (-1), which means 0 for the first stoptime, and the previous value for subsequent stop times.
            if (i == 0) {
                continuousPickups[i] = stopTime.getContinuousPickup() == -1 ? 0 : stopTime.getContinuousPickup();
                continuousDropoffs[i] = stopTime.getContinuousDropOff() == -1 ? 0 : stopTime.getContinuousDropOff();
            } else {
                continuousPickups[i] = stopTime.getContinuousPickup() == -1 ? continuousPickups[i-1] : stopTime.getContinuousPickup();
                continuousDropoffs[i] = stopTime.getContinuousDropOff() == -1 ? continuousDropoffs[i-1] : stopTime.getContinuousDropOff();
            }
        }
        /*
         * TriMet GTFS has many trips that differ only in the pick/drop status of their initial and
         * final stops. This may have something to do with interlining. They are turning pickups off
         * on the final stop of a trip to indicate that there is no interlining, because they supply
         * block IDs for all trips, even those followed by dead runs. See issue 681. Enabling
         * dropoffs at the initial stop and pickups at the final merges similar patterns while
         * having no effect on routing.
         */
        dropoffs[0] = 0;
        pickups[size - 1] = 0;
    }

    /**
     * @param stopId in agency_id format
     */
    public boolean containsStop (String stopId) {
        if (stopId == null) return false;
        for (Stop stop : stops) if (stopId.equals(stop.getId().toString())) return true;
        return false;
    }

    private static final Logger LOG = LoggerFactory.getLogger(StopPattern.class);

    /**
     * In most cases we want to use identity equality for StopPatterns. There is a single StopPattern instance for each
     * semantic StopPattern, and we don't want to calculate complicated hashes or equality values during normal
     * execution. However, in some cases we want a way to consistently identify trips across versions of a GTFS feed, when the
     * feed publisher cannot ensure stable trip IDs. Therefore we define some additional hash functions.
     */
    public HashCode semanticHash(HashFunction hashFunction) {
        Hasher hasher = hashFunction.newHasher();
        for (int s = 0; s < size; s++) {
            Stop stop = stops[s];
            // Truncate the lat and lon to 6 decimal places in case they move slightly between feed versions
            hasher.putLong((long) (stop.getLat() * 1000000));
            hasher.putLong((long) (stop.getLon() * 1000000));
        }
        // Use hops rather than stops because drop-off at stop 0 and pick-up at last stop are not important
        // and have changed between OTP versions.
        for (int hop = 0; hop < size - 1; hop++) {
            hasher.putInt(pickups[hop]);
            hasher.putInt(dropoffs[hop + 1]);
            hasher.putInt(continuousPickups[hop]);
            hasher.putInt(continuousDropoffs[hop + 1]);
        }
        return hasher.hash();
    }

}
