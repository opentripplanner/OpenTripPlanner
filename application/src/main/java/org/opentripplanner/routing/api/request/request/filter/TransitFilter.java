package org.opentripplanner.routing.api.request.request.filter;

import org.opentripplanner.transit.model.network.TripPattern;
import org.opentripplanner.transit.model.timetable.TripTimes;

public interface TransitFilter {
  /**
   * Return false is trip pattern is banned, otherwise return true
   */
  boolean matchTripPattern(TripPattern tripPattern);

  /**
   * Return false is tripTimes are banned, otherwise return true
   */
  boolean matchTripTimes(TripTimes trip);

  /**
   * Returns {@code true} if this filter contains mode-selective constraints that may require
   * per-trip filtering on multi-mode patterns.
   *
   * @see org.opentripplanner.model.modes.AllowTransitModeFilter#isModeSelective()
   */
  default boolean isModeSelective() {
    return false;
  }
}
