package org.opentripplanner.routing.algorithm.filterchain.filters.system;

import java.time.Instant;
import java.util.function.Predicate;
import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.routing.algorithm.filterchain.framework.spi.RemoveItineraryFlagger;

/**
 * The flex router doesn't use the transit router's time window but nevertheless using it
 * for filtering is useful when combining flex with transit.
 * <p>
 * The flex router also searches the previous day (arrive by) or the next one (depart after).
 * If you didn't do it you could get yesterday's or tomorrow's results where you would not expect it.
 */
public class FlexSearchWindowFilter implements RemoveItineraryFlagger {

  public static final String TAG = "outside-flex-window";

  private final Instant earliestDepartureTime;

  public FlexSearchWindowFilter(Instant earliestDepartureTime) {
    this.earliestDepartureTime = earliestDepartureTime;
  }

  @Override
  public String name() {
    return TAG;
  }

  @Override
  public Predicate<Itinerary> shouldBeFlaggedForRemoval() {
    return it -> {
      if (it.isFlexAndWalkOnly()) {
        var time = it.startTime().toInstant();
        return time.isBefore(earliestDepartureTime);
      } else {
        return false;
      }
    };
  }

  @Override
  public boolean skipAlreadyFlaggedItineraries() {
    return false;
  }
}
