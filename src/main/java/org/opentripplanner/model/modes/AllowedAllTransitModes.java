package org.opentripplanner.model.modes;

import org.opentripplanner.transit.model.network.TransitMode;

public class AllowedAllTransitModes implements AllowedTransitMode {

  /**
   * Check if this filter allows the provided TransitMode
   */
  public boolean allows(TransitMode transitMode, String netexSubMode) {
    return true;
  }

  @Override
  public int hashCode() {
    return 293344561;
  }

  @Override
  public boolean equals(Object obj) {
    return obj != null && obj.getClass() == AllowedAllTransitModes.class;
  }

  @Override
  public String toString() {
    return "AllowedAllTransitModes{}";
  }
}
