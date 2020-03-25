package org.opentripplanner.routing.algorithm.raptor.transit.request;

import org.opentripplanner.model.FeedScopedId;
import org.opentripplanner.routing.algorithm.raptor.transit.TripPatternWithRaptorStopIndexes;

public class TripPatternWithId extends TripPatternWithRaptorStopIndexes {
  private FeedScopedId id;

  public TripPatternWithId(
      FeedScopedId id,
      int[] stopIndexes,
      org.opentripplanner.model.TripPattern originalTripPattern
  ) {
    super(stopIndexes, originalTripPattern);
    this.id = id;
  }

  @Override
  public FeedScopedId getId() {
    return id;
  }
}
