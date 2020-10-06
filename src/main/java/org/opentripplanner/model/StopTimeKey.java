package org.opentripplanner.model;

/**
 * This class is used as a reference to a StopTime wrapping the {@link Trip#getId()} and {@code stopSequence}.
 * StopTimes instances do not exist in the graph as entities, they are represented by
 * the {@link org.opentripplanner.routing.trippattern.TripTimes}, but we use this class to map other entities
 * (NoticeAssignment) to StopTimes to be able to decorate itineraries with such data.
 */
public class StopTimeKey extends TransitEntity<String> {

    private final FeedScopedId tripId;
    private final int stopSequenceNumber;

    public StopTimeKey(FeedScopedId tripId, int stopSequenceNumber) {
        this.tripId = tripId;
        this.stopSequenceNumber = stopSequenceNumber;
    }

    public String getId() {
        return this.stopSequenceNumber + ";" + tripId.toString();
    }

    @Override
    public String toString() {
        return "StopTimeKey<" + tripId.toString() + "_" + stopSequenceNumber + ">";
    }
}
