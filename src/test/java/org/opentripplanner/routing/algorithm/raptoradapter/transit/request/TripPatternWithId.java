package org.opentripplanner.routing.algorithm.raptoradapter.transit.request;

import org.opentripplanner.routing.algorithm.raptoradapter.transit.TripPatternWithRaptorStopIndexes;
import org.opentripplanner.transit.model.basic.FeedScopedId;

public class TripPatternWithId extends TripPatternWithRaptorStopIndexes {

  private final FeedScopedId id;

  public TripPatternWithId(
    FeedScopedId id,
    int[] stopIndexes,
    org.opentripplanner.model.TripPattern originalTripPattern
  ) {
    super(originalTripPattern, stopIndexes);
    this.id = id;
  }

  @Override
  public FeedScopedId getId() {
    return id;
  }
}
