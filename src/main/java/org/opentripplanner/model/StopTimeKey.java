package org.opentripplanner.model;

import java.util.Objects;

/**
 * This class is used as a reference to a StopTime wrapping the {@link Trip#getId()} and {@code stopSequence}.
 * StopTimes instances do not exist in the graph as entities, they are represented by
 * the {@link org.opentripplanner.routing.trippattern.TripTimes}, but we use this class to map other entities
 * (NoticeAssignment) to StopTimes to be able to decorate itineraries with such data.
 */
public class StopTimeKey extends TransitEntity<StopTimeKey> {

    private final FeedScopedId tripId;
    private final int stopSequenceNumber;

    public StopTimeKey(FeedScopedId tripId, int stopSequenceNumber) {
        this.tripId = tripId;
        this.stopSequenceNumber = stopSequenceNumber;
    }

    @Override
    public StopTimeKey getId() {
        return this;
    }

    @Override
    public String toString() {
        return "StopTimeKey<" + tripId.toString() + "_" + stopSequenceNumber + ">";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        StopTimeKey that = (StopTimeKey) o;
        return stopSequenceNumber == that.stopSequenceNumber && tripId.equals(that.tripId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(tripId, stopSequenceNumber);
    }
}
