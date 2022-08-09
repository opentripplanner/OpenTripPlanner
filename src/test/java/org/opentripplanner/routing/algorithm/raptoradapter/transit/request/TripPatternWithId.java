package org.opentripplanner.routing.algorithm.raptoradapter.transit.request;

import org.opentripplanner.routing.algorithm.raptoradapter.transit.TripPatternWithRaptorStopIndexes;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.transit.model.network.TripPattern;

public class TripPatternWithId extends TripPatternWithRaptorStopIndexes {

  private final FeedScopedId id;

  public TripPatternWithId(FeedScopedId id, int[] stopIndexes, TripPattern originalTripPattern) {
    super(originalTripPattern, stopIndexes);
    this.id = id;
  }

  @Override
  public FeedScopedId getId() {
    return id;
  }
}
