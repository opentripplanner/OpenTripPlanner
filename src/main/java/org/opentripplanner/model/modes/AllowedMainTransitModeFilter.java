package org.opentripplanner.model.modes;

import org.opentripplanner.transit.model.network.TransitMode;

class AllowedMainTransitModeFilter implements AllowedTransitModeFilter {

  private final TransitMode mainMode;

  public AllowedMainTransitModeFilter(TransitMode mainMode) {
    this.mainMode = mainMode;
  }

  public boolean allows(TransitMode transitMode, String netexSubMode) {
    return mainMode == transitMode;
  }

  @Override
  public int hashCode() {
    return mainMode.hashCode() + 176393;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    AllowedMainTransitModeFilter that = (AllowedMainTransitModeFilter) o;
    return mainMode == that.mainMode;
  }

  @Override
  public String toString() {
    return "AllowedMainTransitModeFilter{ mainMode: " + mainMode + "}";
  }
}
