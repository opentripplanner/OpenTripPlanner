package org.opentripplanner.routing.algorithm.filterchain.filters.system;

import java.time.Duration;
import java.time.Instant;
import java.util.function.Predicate;
import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.model.plan.SortOrder;
import org.opentripplanner.routing.algorithm.filterchain.framework.spi.RemoveItineraryFlagger;

/**
 * The flex router doesn't use the transit router's  search-window, but nevertheless using it
 * for filtering is useful when combining flex with transit.
 * <p>
 * The flex router also searches the previous day (arrive by) or the next one (depart after).
 * If you didn't filter the flex results by something you could get yesterday's or tomorrow's
 * trips where you would not expect it.
 */
public class FlexSearchWindowFilter implements RemoveItineraryFlagger {

  public static final String TAG = "flex-outside-search-window";

  private final Instant earliestDepartureTime;
  private final Instant latestArrivalTime;
  private final SortOrder sortOrder;

  public FlexSearchWindowFilter(
    Instant earliestDepartureTime,
    Duration searchWindow,
    SortOrder sortOrder
  ) {
    this.earliestDepartureTime = earliestDepartureTime;
    this.latestArrivalTime = earliestDepartureTime.plus(searchWindow);
    this.sortOrder = sortOrder;
  }

  @Override
  public String name() {
    return TAG;
  }

  @Override
  public Predicate<Itinerary> shouldBeFlaggedForRemoval() {
    return it -> {
      if (it.isDirectFlex()) {
        return switch (sortOrder) {
          case STREET_AND_DEPARTURE_TIME -> {
            var time = it.startTime().toInstant();
            yield time.isBefore(earliestDepartureTime);
          }
          case STREET_AND_ARRIVAL_TIME -> {
            var time = it.startTime().toInstant();
            yield time.isAfter(latestArrivalTime);
          }
        };
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
