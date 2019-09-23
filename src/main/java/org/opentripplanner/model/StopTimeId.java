package org.opentripplanner.model;

import java.io.Serializable;

/**
 * The StopTime id is the an aggregated field from {@link Trip#getId()} and {@code stopSequence}.
 */
public class StopTimeId implements Serializable {
    private final FeedScopedId tripId;
    private final int stopSequensNo;

    public StopTimeId(FeedScopedId tripId, int stopSequensNo) {
        this.tripId = tripId;
        this.stopSequensNo = stopSequensNo;
    }

    @Override public int hashCode() { return stopSequensNo + 37 * tripId.hashCode(); }
    @Override public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;

        StopTimeId other = (StopTimeId) obj;
        return stopSequensNo == other.stopSequensNo && tripId.equals(other.tripId);
    }
    @Override public String toString() { return tripId.toString() + ":" + stopSequensNo; }
}
