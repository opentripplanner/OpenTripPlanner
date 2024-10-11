package org.opentripplanner.transit.model.timetable;

import org.opentripplanner.transit.model.framework.AbstractTransitEntity;
import org.opentripplanner.transit.model.framework.FeedScopedId;

/**
 * This class is used as a reference to a StopTime wrapping the {@link Trip#getId()} and
 * {@code stopSequence}. StopTimes instances do not exist in the graph as entities, they are
 * represented by the {@link TripTimes}, but we use this class to map other entities
 * (NoticeAssignment) to StopTimes to be able to decorate itineraries with such data.
 */
public class StopTimeKey extends AbstractTransitEntity<StopTimeKey, StopTimeKeyBuilder> {

  StopTimeKey(StopTimeKeyBuilder builder) {
    super(builder.getId());
  }

  public static StopTimeKeyBuilder of(FeedScopedId tripId, int stopSequenceNumber) {
    return new StopTimeKeyBuilder(
      new FeedScopedId(tripId.getFeedId(), tripId.getId() + "_#" + stopSequenceNumber)
    );
  }

  @Override
  public boolean sameAs(StopTimeKey other) {
    return getId().equals(other.getId());
  }

  @Override
  public StopTimeKeyBuilder copy() {
    return new StopTimeKeyBuilder(this);
  }
}
