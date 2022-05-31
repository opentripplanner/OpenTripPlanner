package org.opentripplanner.model.modes;

import java.util.Objects;
import javax.annotation.Nonnull;
import org.opentripplanner.transit.model.network.TransitMode;

public class AllowedMainAndSubTransitMode implements AllowedTransitMode {

  private final TransitMode mainMode;

  private final String subMode;

  public AllowedMainAndSubTransitMode(@Nonnull TransitMode mainMode, @Nonnull String subMode) {
    this.mainMode = mainMode;
    this.subMode = subMode;
  }

  /**
   * Check if this filter allows the provided TransitMode
   */
  public boolean allows(TransitMode transitMode, String netexSubMode) {
    return mainMode == transitMode && subMode.equals(netexSubMode);
  }

  @Override
  public int hashCode() {
    return Objects.hash(mainMode, subMode);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    AllowedMainAndSubTransitMode that = (AllowedMainAndSubTransitMode) o;
    return mainMode == that.mainMode && subMode.equals(that.subMode);
  }

  @Override
  public String toString() {
    return "AllowedMainAndSubTransitMode{ mainMode: " + mainMode + ", subMode: " + subMode + "}";
  }
}
