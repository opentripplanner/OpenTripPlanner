package org.opentripplanner.routing.algorithm.raptoradapter.transit.request;

import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.transit.model.network.RoutingTripPattern;
import org.opentripplanner.transit.model.network.TripPattern;

public class TripPatternWithId extends RoutingTripPattern {

  private final FeedScopedId id;

  public TripPatternWithId(FeedScopedId id, TripPattern originalTripPattern) {
    super(originalTripPattern);
    this.id = id;
  }

  @Override
  public FeedScopedId getId() {
    return id;
  }
}
