package org.opentripplanner.model.modes;

import org.opentripplanner.transit.model.network.TransitMode;

public class AllowedMainTransitMode implements AllowedTransitMode {

  private final TransitMode mainMode;

  public AllowedMainTransitMode(TransitMode mainMode) {
    this.mainMode = mainMode;
  }

  /**
   * Check if this filter allows the provided TransitMode
   */
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
    AllowedMainTransitMode that = (AllowedMainTransitMode) o;
    return mainMode == that.mainMode;
  }

  @Override
  public String toString() {
    return "AllowedMainTransitMode{ mainMode: " + mainMode + "}";
  }
}
