package org.opentripplanner.routing.algorithm.raptor.transit.request;

import org.opentripplanner.model.FeedScopedId;
import org.opentripplanner.routing.algorithm.raptor.transit.TripPattern;
import org.opentripplanner.routing.algorithm.raptor.transit.TripSchedule;
import org.opentripplanner.routing.core.TraverseMode;

import java.util.List;

public class TripPatternWithId extends TripPattern {
  private FeedScopedId id;

  public TripPatternWithId(
      FeedScopedId id,
      List<TripSchedule> tripSchedules,
      TraverseMode transitMode,
      int[] stopIndexes,
      org.opentripplanner.model.TripPattern originalTripPattern
  ) {
    super(tripSchedules, transitMode, stopIndexes, originalTripPattern);
    this.id = id;
  }

  @Override
  public FeedScopedId getId() {
    return id;
  }
}
