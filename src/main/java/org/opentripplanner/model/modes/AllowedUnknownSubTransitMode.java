package org.opentripplanner.model.modes;

import org.opentripplanner.transit.model.network.TransitMode;

public class AllowedUnknownSubTransitMode implements AllowedTransitMode {

  private final TransitMode mainMode;

  public AllowedUnknownSubTransitMode(TransitMode mainMode) {
    this.mainMode = mainMode;
  }

  /**
   * Check if this filter allows the provided TransitMode
   */
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
    AllowedUnknownSubTransitMode that = (AllowedUnknownSubTransitMode) o;
    return mainMode == that.mainMode;
  }

  @Override
  public String toString() {
    return "AllowedUnknownSubTransitMode{ mainMode: " + mainMode + "}";
  }
}
