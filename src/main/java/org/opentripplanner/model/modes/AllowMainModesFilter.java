package org.opentripplanner.model.modes;

import java.util.Collection;
import java.util.EnumSet;
import java.util.Set;
import org.opentripplanner.framework.tostring.ToStringBuilder;
import org.opentripplanner.transit.model.basic.SubMode;
import org.opentripplanner.transit.model.basic.TransitMode;

class AllowMainModesFilter implements AllowTransitModeFilter {

  private final Set<TransitMode> mainModes;

  AllowMainModesFilter(Collection<AllowMainModeFilter> filters) {
    this.mainModes = EnumSet.copyOf(filters.stream().map(AllowMainModeFilter::mainMode).toList());
  }

  @Override
  public boolean allows(TransitMode transitMode, SubMode ignore) {
    return mainModes.contains(transitMode);
  }

  @Override
  public int hashCode() {
    return mainModes.hashCode() + 176393;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    AllowMainModesFilter that = (AllowMainModesFilter) o;
    return mainModes.equals(that.mainModes);
  }

  @Override
  public String toString() {
    return ToStringBuilder.of(AllowMainModesFilter.class).addCol("mainModes", mainModes).toString();
  }
}
