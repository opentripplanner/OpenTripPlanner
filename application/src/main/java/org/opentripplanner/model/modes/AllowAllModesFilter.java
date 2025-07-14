package org.opentripplanner.model.modes;

import org.opentripplanner.transit.model.basic.SubMode;
import org.opentripplanner.transit.model.basic.TransitMode;

class AllowAllModesFilter implements AllowTransitModeFilter {

  @Override
  public boolean match(TransitMode transitMode, SubMode netexSubMode) {
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
    return AllowAllModesFilter.class.getSimpleName();
  }
}
