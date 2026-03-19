package org.opentripplanner.model.modes;

import org.opentripplanner.transit.model.basic.NarrowedTransitMode;
import org.opentripplanner.transit.model.basic.SubMode;
import org.opentripplanner.transit.model.basic.TransitMode;
import org.opentripplanner.transit.service.ReplacementHelper;

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
    if (mode.getSubMode() != null && mode.getSubMode() == netexSubmode) {
      return true;
    }
    return ReplacementHelper.isReplacementExtendedType(gtfsExtendedType);
  }
}
