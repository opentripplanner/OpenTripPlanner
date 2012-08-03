package org.opentripplanner.routing.trippattern;

import org.onebusaway.gtfs.model.AgencyAndId;

public class Update implements Comparable<Update> {

    final AgencyAndId tripId;
    final String stopId;
    final int stopSeq;
    final int arrive; // sec since midnight
    final int depart; // sec since midnight

    public Update (AgencyAndId tripId, String stopId, int stopSeq, int arrive, int depart) {
        this.tripId = tripId;
        this.stopId = stopId;
        this.stopSeq = stopSeq;
        this.arrive = arrive;
        this.depart = depart;
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
        return String.format("%s %s %d %s %s", tripId, stopId, stopSeq, arrive, depart);
    }
}