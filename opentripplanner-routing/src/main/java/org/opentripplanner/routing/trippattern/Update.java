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

    @Override
    public int compareTo(Update other) {
        int result;
        result = this.tripId.compareTo(other.tripId);
        if (result != 0)
            return result;
        result = this.stopSeq - other.stopSeq;
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