package org.opentripplanner.routing.algorithm.filterchain.filters.system;

import java.time.Duration;
import java.time.Instant;
import java.util.function.Predicate;
import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.routing.algorithm.filterchain.framework.spi.RemoveItineraryFlagger;

/**
 * This filter will remove all itineraries that are outside the search-window. In some
 * cases the access is time-shifted after the end of the search-window. These results
 * should appear again when paging to the next page. Hence, this filter will remove
 * such itineraries. The same is true for when paging to the previous page for arriveBy=true.
 * <p>
 * Itineraries matching the start(earliest-departure-time) are included and itineraries matching
 * the end(latest-departure-time) are not. The filter is {@code [inclusive, exclusive]}.
 */
public class OutsideSearchWindowFilter implements RemoveItineraryFlagger {

  public static final String TAG = "outside-search-window";

  private final Instant earliestDepartureTime;
  private final Instant latestDepartureTime;

  public OutsideSearchWindowFilter(Instant earliestDepartureTime, Duration searchWindow) {
    this.earliestDepartureTime = earliestDepartureTime;
    this.latestDepartureTime = earliestDepartureTime.plus(searchWindow);
  }

  @Override
  public String name() {
    return TAG;
  }

  @Override
  public Predicate<Itinerary> shouldBeFlaggedForRemoval() {
    return it -> {
      var time = it.startTime().toInstant();
      return time.isBefore(earliestDepartureTime) || !time.isBefore(latestDepartureTime);
    };
  }

  @Override
  public boolean skipAlreadyFlaggedItineraries() {
    return false;
  }

  /**
   * Return {@code true} if given {@code itinerary} is tagged by this filter.
   */
  public static boolean taggedBy(Itinerary itinerary) {
    return itinerary.hasSystemNoticeTag(TAG);
  }
}
