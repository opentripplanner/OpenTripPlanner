package org.opentripplanner.model.modes;

import java.util.Objects;
import javax.annotation.Nullable;
import org.opentripplanner.transit.model.basic.MainAndSubMode;
import org.opentripplanner.transit.model.basic.SubMode;
import org.opentripplanner.transit.model.basic.TransitMode;
import org.opentripplanner.utils.tostring.ToStringBuilder;

class AllowMainAndSubModeFilter implements AllowTransitModeFilter {

  private final TransitMode mainMode;

  private final SubMode subMode;

  AllowMainAndSubModeFilter(TransitMode mainMode, @Nullable SubMode subMode) {
    this.mainMode = mainMode;
    this.subMode = subMode;
  }

  AllowMainAndSubModeFilter(MainAndSubMode mode) {
    this(mode.mainMode(), mode.subMode());
  }

  @Override
  public boolean isSubMode() {
    return true;
  }

  @Override
  public boolean match(TransitMode transitMode, SubMode netexSubMode) {
    // SubModes are deduplicated, so it is safe to use "==" here
    return mainMode == transitMode && subMode == netexSubMode;
  }

  TransitMode mainMode() {
    return mainMode;
  }

  @Nullable
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
    return ToStringBuilder.of(AllowMainAndSubModeFilter.class)
      .addEnum("mainMode", mainMode)
      .addObj("subMode", subMode)
      .toString();
  }
}
