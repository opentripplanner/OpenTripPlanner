package org.opentripplanner.model.modes;

import org.opentripplanner.framework.tostring.ToStringBuilder;
import org.opentripplanner.transit.model.basic.SubMode;
import org.opentripplanner.transit.model.basic.TransitMode;

class AllowMainModeFilter implements AllowTransitModeFilter {

  private final TransitMode mainMode;

  AllowMainModeFilter(TransitMode mainMode) {
    this.mainMode = mainMode;
  }

  TransitMode mainMode() {
    return mainMode;
  }

  @Override
  public boolean allows(TransitMode transitMode, SubMode ignore) {
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
    AllowMainModeFilter that = (AllowMainModeFilter) o;
    return mainMode == that.mainMode;
  }

  @Override
  public String toString() {
    return ToStringBuilder.of(AllowMainModeFilter.class).addEnum("mainMode", mainMode).toString();
  }
}
