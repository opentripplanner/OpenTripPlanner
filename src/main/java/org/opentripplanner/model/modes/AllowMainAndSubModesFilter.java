package org.opentripplanner.model.modes;

import java.util.BitSet;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import org.opentripplanner.transit.model.network.SubMode;
import org.opentripplanner.transit.model.network.TransitMode;
import org.opentripplanner.util.lang.ToStringBuilder;

class AllowMainAndSubModesFilter implements AllowTransitModeFilter {

  private final TransitMode mainMode;

  private final BitSet subModes;

  @SuppressWarnings("ConstantConditions")
  AllowMainAndSubModesFilter(Collection<AllowMainAndSubModeFilter> filters) {
    this.mainMode = filters.iterator().next().mainMode();
    this.subModes = new BitSet();
    for (var it : filters) {
      if (it.mainMode() != this.mainMode) {
        throw new IllegalArgumentException("Main mode is not the same: " + filters);
      }
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
    // Sort the list of sub-modes to make sure the order is the same every time, this
    // allows using the toString in tests
    List<SubMode> subModes = SubMode
      .getByIndex(this.subModes)
      .stream()
      .sorted(Comparator.comparing(SubMode::name))
      .toList();

    return ToStringBuilder
      .of(AllowMainAndSubModesFilter.class)
      .addEnum("mainMode", mainMode)
      .addCol("subModes", subModes)
      .toString();
  }
}
