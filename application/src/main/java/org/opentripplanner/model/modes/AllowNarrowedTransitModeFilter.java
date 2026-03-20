package org.opentripplanner.model.modes;

import java.util.Objects;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.opentripplanner.transit.model.basic.NarrowedTransitMode;
import org.opentripplanner.transit.model.basic.SubMode;
import org.opentripplanner.transit.model.basic.TransitMode;
import org.opentripplanner.transit.service.ReplacementHelper;
import org.opentripplanner.utils.tostring.ToStringBuilder;

public class AllowNarrowedTransitModeFilter implements AllowTransitModeFilter {

  NarrowedTransitMode mode;

  public AllowNarrowedTransitModeFilter(NarrowedTransitMode mode) {
    this.mode = mode;
  }

  @Override
  public boolean isModeSelective() {
    return true;
  }

  @Override
  public boolean match(TransitMode transitMode, SubMode netexSubmode, Integer gtfsExtendedType) {
    if (mode.getMode() != transitMode) {
      return false;
    }
    return (
      (Boolean.TRUE.equals(mode.isReplacement()) &&
        ReplacementHelper.isReplacement(netexSubmode, gtfsExtendedType)) ||
      (mode.getSubMode() != null && mode.getSubMode() == netexSubmode)
    );
  }

  @Override
  public int hashCode() {
    return Objects.hash(mode.getMode(), mode.getSubMode(), mode.isReplacement());
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    AllowNarrowedTransitModeFilter that = (AllowNarrowedTransitModeFilter) o;
    return new EqualsBuilder()
      .append(mode.getMode(), that.mode.getMode())
      .append(mode.getSubMode(), that.mode.getSubMode())
      .append(mode.isReplacement(), that.mode.isReplacement())
      .isEquals();
  }

  @Override
  public String toString() {
    return ToStringBuilder.of(AllowNarrowedTransitModeFilter.class)
      .addEnum("mode", mode.getMode())
      .addObj("subMode", mode.getSubMode())
      .addObj("isReplacement", mode.isReplacement())
      .toString();
  }
}
