package org.opentripplanner.routing.trippattern;

import org.onebusaway.gtfs.model.AgencyAndId;

public class Update implements Comparable<Update> {

    final AgencyAndId tripId;
    final String stopId;
    final int stopSeq;
    protected int arrive; // sec since midnight
    final int depart; // sec since midnight
    final Status status;
    
    public Update (AgencyAndId tripId, String stopId, int stopSeq, int arrive, int depart, Status status) {
        this.tripId = tripId;
        this.stopId = stopId;
        this.stopSeq = stopSeq;
        this.arrive = arrive;
        this.depart = depart;
        this.status = status;
    }

    /**
     * This comparator is useful for breaking lists of mixed-trip updates into single-trip blocks.
     * We sort on (tripId, stopSequence, departureTime) because there may be duplicate stops in an
     * update list, and we want them to be in a predictable order for filtering.
     */
    @Override
    public int compareTo(Update other) {
        int result;
        result = this.tripId.compareTo(other.tripId);
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
        PASSED,
        ARRIVED,
        PREDICTION,
        UNKNOWN
    }
    
}