package org.opentripplanner.model.modes;

import java.util.Collection;
import java.util.Objects;
import javax.annotation.Nullable;
import org.opentripplanner.transit.model.basic.SubMode;
import org.opentripplanner.transit.model.basic.TransitMode;
import org.opentripplanner.utils.tostring.ToStringBuilder;

public class AllowNarrowedTransitModesFilter implements AllowTransitModeFilter {

  private final TransitMode mainMode;

  private final Collection<AllowNarrowedTransitModeFilter> filters;

  AllowNarrowedTransitModesFilter(Collection<AllowNarrowedTransitModeFilter> filters) {
    this.mainMode = filters.iterator().next().mainMode();
    this.filters = filters;
  }

  @Override
  public boolean isModeSelective() {
    return true;
  }

  @Override
  public boolean match(
    TransitMode transitMode,
    SubMode subMode,
    @Nullable Integer gtfsExtendedType
  ) {
    return (
      mainMode == transitMode &&
      filters.stream().anyMatch(filter -> filter.match(transitMode, subMode, gtfsExtendedType))
    );
  }

  @Override
  public int hashCode() {
    return Objects.hash(mainMode, filters);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }
    AllowNarrowedTransitModesFilter that = (AllowNarrowedTransitModesFilter) obj;
    return mainMode == that.mainMode && filters.equals(that.filters);
  }

  @Override
  public String toString() {
    return ToStringBuilder.of(AllowNarrowedTransitModesFilter.class)
      .addEnum("mainMode", mainMode)
      .addCol("filters", filters)
      .toString();
  }
}
