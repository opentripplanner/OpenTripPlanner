package org.opentripplanner.model.modes;

import java.util.BitSet;
import java.util.Collection;
import java.util.Objects;
import org.opentripplanner.transit.model.network.SubMode;
import org.opentripplanner.transit.model.network.TransitMode;
import org.opentripplanner.util.lang.ToStringBuilder;

class AllowMainAndSubModesFilter implements AllowTransitModeFilter {

  private final TransitMode mainMode;

  private final BitSet subModes;

  AllowMainAndSubModesFilter(Collection<AllowMainAndSubModeFilter> filters) {
    this.mainMode = filters.iterator().next().mainMode();
    this.subModes = new BitSet();
    for (var it : filters) {
      this.subModes.set(it.subMode().index());
    }
  }

  @Override
  public boolean allows(TransitMode transitMode, SubMode netexSubMode) {
    // SubModes are deduplicated, so it is safe to use "==" here
    return mainMode == transitMode && subModes.get(netexSubMode.index());
  }

  @Override
  public int hashCode() {
    return Objects.hash(mainMode, subModes);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    AllowMainAndSubModesFilter that = (AllowMainAndSubModesFilter) o;
    return mainMode == that.mainMode && subModes.equals(that.subModes);
  }

  @Override
  public String toString() {
    return ToStringBuilder
      .of(AllowMainAndSubModesFilter.class)
      .addEnum("mainMode", mainMode)
      .addCol("subModes", SubMode.getByIndex(subModes))
      .toString();
  }
}
