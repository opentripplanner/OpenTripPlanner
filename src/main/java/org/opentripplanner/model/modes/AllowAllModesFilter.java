package org.opentripplanner.model.modes;

import org.opentripplanner.transit.model.network.SubMode;
import org.opentripplanner.transit.model.network.TransitMode;

class AllowAllModesFilter implements AllowTransitModeFilter {

  @Override
  public boolean allows(TransitMode transitMode, SubMode netexSubMode) {
    return true;
  }

  @Override
  public int hashCode() {
    return 293344561;
  }

  @Override
  public boolean equals(Object obj) {
    return obj != null && obj.getClass() == AllowAllModesFilter.class;
  }

  @Override
  public String toString() {
    return "AllowAllModesFilter{}";
  }
}
