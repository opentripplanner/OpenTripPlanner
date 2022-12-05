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

  // TODO: 2022-12-05 this is not the best name since this interface is used both for whitelist and blacklist
  /**
   * Check if this filter allows the provided TransitMode
   */
  boolean allows(TransitMode transitMode, SubMode netexSubMode);
}
