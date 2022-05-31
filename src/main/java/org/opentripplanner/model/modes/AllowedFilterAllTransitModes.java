package org.opentripplanner.model.modes;

import org.opentripplanner.transit.model.network.TransitMode;

class AllowedFilterAllTransitModes implements AllowedTransitModeFilter {

  public boolean allows(TransitMode transitMode, String netexSubMode) {
    return true;
  }

  @Override
  public int hashCode() {
    return 293344561;
  }

  @Override
  public boolean equals(Object obj) {
    return obj != null && obj.getClass() == AllowedFilterAllTransitModes.class;
  }

  @Override
  public String toString() {
    return "AllowedFilterAllTransitModes{}";
  }
}
