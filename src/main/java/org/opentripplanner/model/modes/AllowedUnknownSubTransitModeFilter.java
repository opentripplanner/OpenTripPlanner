package org.opentripplanner.model.modes;

import org.opentripplanner.transit.model.network.TransitMode;

class AllowedUnknownSubTransitModeFilter implements AllowedTransitModeFilter {

  private final TransitMode mainMode;

  public AllowedUnknownSubTransitModeFilter(TransitMode mainMode) {
    this.mainMode = mainMode;
  }

  public boolean allows(TransitMode transitMode, String netexSubMode) {
    return mainMode == transitMode && netexSubMode == null;
  }

  @Override
  public int hashCode() {
    return mainMode.hashCode() + 275347561;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    AllowedUnknownSubTransitModeFilter that = (AllowedUnknownSubTransitModeFilter) o;
    return mainMode == that.mainMode;
  }

  @Override
  public String toString() {
    return "AllowedUnknownSubTransitModeFilter{ mainMode: " + mainMode + "}";
  }
}
