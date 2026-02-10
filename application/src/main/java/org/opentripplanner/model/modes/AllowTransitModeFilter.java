package org.opentripplanner.model.modes;

import java.io.Serializable;
import java.util.Collection;
import org.opentripplanner.transit.model.basic.MainAndSubMode;
import org.opentripplanner.transit.model.basic.SubMode;
import org.opentripplanner.transit.model.basic.TransitMode;

/**
 * Used to filter out modes for routing requests.
 */
public interface AllowTransitModeFilter extends Serializable {
  static AllowTransitModeFilter of(Collection<MainAndSubMode> modes) {
    return FilterFactory.create(modes);
  }

  /**
   * Check if this filter allows the provided TransitMode
   */
  boolean match(TransitMode transitMode, SubMode netexSubMode);

  /**
   * Returns true if this filter needs to perform trip-level (TripTimes) filtering
   * when a TripPattern contains multiple modes. When a pattern has trips with
   * different modes or submodes, pattern-level filtering alone is insufficient
   * and we must check each trip individually.
   */
  default boolean matchesOnTripLevel() {
    return false;
  }
}
