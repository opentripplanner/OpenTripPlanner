package org.opentripplanner.model.modes;

import java.io.Serializable;
import java.util.Collection;
import java.util.Set;
import org.opentripplanner.transit.model.network.MainAndSubMode;
import org.opentripplanner.transit.model.network.SubMode;
import org.opentripplanner.transit.model.network.TransitMode;

/**
 * Used to filter out modes for routing requests.
 */
public interface AllowTransitModeFilter extends Serializable {
  static Set<AllowTransitModeFilter> of(Collection<MainAndSubMode> modes) {
    return FilterFactory.create(modes);
  }

  /**
   * Check if this filter allows the provided TransitMode
   */
  boolean allows(TransitMode transitMode, SubMode netexSubMode);
}
