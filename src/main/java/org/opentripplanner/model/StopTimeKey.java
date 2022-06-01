package org.opentripplanner.model;

import org.opentripplanner.transit.model.basic.FeedScopedId;
import org.opentripplanner.transit.model.basic.TransitEntity;
import org.opentripplanner.transit.model.timetable.Trip;

/**
 * This class is used as a reference to a StopTime wrapping the {@link Trip#getId()} and {@code
 * stopSequence}. StopTimes instances do not exist in the graph as entities, they are represented by
 * the {@link org.opentripplanner.routing.trippattern.TripTimes}, but we use this class to map other
 * entities (NoticeAssignment) to StopTimes to be able to decorate itineraries with such data.
 */
public class StopTimeKey extends TransitEntity {

  public StopTimeKey(FeedScopedId tripId, int stopSequenceNumber) {
    super(new FeedScopedId(tripId.getFeedId(), tripId.getId() + "_#" + stopSequenceNumber));
  }

  @Override
  public String toString() {
    return "StopTimeKey<" + getId() + ">";
  }
}
