package org.opentripplanner.routing.algorithm.filterchain.deletionflagger;

import java.time.Instant;
import java.util.function.Predicate;
import org.opentripplanner.model.plan.Itinerary;

public class LatestDepartureTimeFilter implements ItineraryDeletionFlagger {

  public static final String TAG = "latest-departure-time-limit";

  private final Instant limit;

  public LatestDepartureTimeFilter(Instant latestDepartureTime) {
    this.limit = latestDepartureTime;
  }

  @Override
  public String name() {
    return TAG;
  }

  @Override
  public Predicate<Itinerary> shouldBeFlaggedForRemoval() {
    return it -> it.startTime().toInstant().isAfter(limit);
  }

  @Override
  public boolean skipAlreadyFlaggedItineraries() {
    return false;
  }
}
