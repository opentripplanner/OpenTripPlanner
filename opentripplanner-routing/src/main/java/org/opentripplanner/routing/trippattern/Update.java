package org.opentripplanner.routing.trippattern;

import org.onebusaway.gtfs.model.AgencyAndId;

public class Update implements Comparable<Update> {

    // these fields can eventually be protected if trippattern is in the same package as update
    public final AgencyAndId tripId;
    public final String stopId;
    public final int stopSeq;
    public int arrive; // sec since midnight
    public final int depart; // sec since midnight
    public final Status status;
    /** The official timestamp for the update, if one was provided, or the time it was received. */
    public final long timestamp;
    
    public Update (AgencyAndId tripId, String stopId, int stopSeq, int arrive, int depart, 
            Status status, long timestamp) {
        this.tripId = tripId;
        this.stopId = stopId;
        this.stopSeq = stopSeq;
        this.arrive = arrive;
        this.depart = depart;
        this.status = status;
        this.timestamp = timestamp;
    }

    /**
     * This ordering is useful for breaking lists of mixed-trip updates into single-trip blocks.
     * We sort on (tripId, timestamp, stopSequence, depart) because there may be duplicate stops in 
     * an update list, and we want them to be in a predictable order for filtering. Usually 
     * duplicate stops are due to multiple updates for the same trip in the same message. In this 
     * case the two updates will have different timestamps, and we want to apply them in order.
     */
    @Override
    public int compareTo(Update other) {
        int result;
        result = this.tripId.compareTo(other.tripId);
        if (result != 0)
            return result;
        result = (int) (this.timestamp - other.timestamp);
        if (result != 0)
            return result;
        result = this.stopSeq - other.stopSeq;
        if (result != 0)
            return result;
        result = this.depart - other.depart;
        return result;
    }

    @Override
    public String toString() {
        return String.format("Update trip %s Stop #%d:%s (%s) A%s D%s", 
                tripId, stopSeq, stopId, status, arrive, depart);
    }
    
    public static enum Status {
        PASSED,     // the vehicle has passed this stop, no point trying to board there.
        ARRIVED,    // the vehicle is at this stop. you could still board right now.
        PREDICTION, // the vehicle is expected to arrive/depart at the indicated times.
        CANCEL,     // the vehicle will not pick up or drop off passengers at this stop.
        PLANNED,    // trip is scheduled to be happening now, but is not broadcasting predictions
        UNKNOWN     // vehicle be broadcasting predictions according to the schedule but it is not
                    // also used if the bus is too far from the planned route (shapes.txt)
    }
    
}