package org.opentripplanner.model.modes;

import java.util.Objects;
import javax.annotation.Nonnull;
import org.opentripplanner.transit.model.network.SubMode;
import org.opentripplanner.transit.model.network.TransitMode;
import org.opentripplanner.util.lang.ToStringBuilder;

class AllowMainAndSubModeFilter implements AllowTransitModeFilter {

  private final TransitMode mainMode;

  private final SubMode subMode;

  AllowMainAndSubModeFilter(@Nonnull TransitMode mainMode, @Nonnull SubMode subMode) {
    this.mainMode = mainMode;
    this.subMode = subMode;
  }

  @Override
  public boolean allows(TransitMode transitMode, SubMode netexSubMode) {
    // SubModes are deduplicated, so it is safe to use "==" here
    return mainMode == transitMode && subMode == netexSubMode;
  }

  TransitMode mainMode() {
    return mainMode;
  }

  SubMode subMode() {
    return subMode;
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
    AllowMainAndSubModeFilter that = (AllowMainAndSubModeFilter) o;
    return mainMode == that.mainMode && subMode.equals(that.subMode);
  }

  @Override
  public String toString() {
    return ToStringBuilder
      .of(AllowMainAndSubModeFilter.class)
      .addEnum("mainMode", mainMode)
      .addObj("subMode", subMode)
      .toString();
  }
}
