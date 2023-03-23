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

  default boolean isSubMode() {
    return false;
  }
}
