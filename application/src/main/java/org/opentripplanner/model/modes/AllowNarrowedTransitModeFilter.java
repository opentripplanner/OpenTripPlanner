package org.opentripplanner.model.modes;

import org.opentripplanner.transit.model.basic.NarrowedTransitMode;
import org.opentripplanner.transit.model.basic.SubMode;
import org.opentripplanner.transit.model.basic.TransitMode;

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
    // TODO
    if (mode.getMode() != transitMode) {
      return false;
    }
    if (mode.getSubModes() != null && !mode.getSubModes().isEmpty()) {
      if (mode.getSubModes().contains(netexSubmode)) {
        return true;
      }
    } else if (mode.getAllowedExtendedTypes() != null) {
      return mode.getAllowedExtendedTypes().contains(gtfsExtendedType);
    } else if (mode.getForbiddenExtendedTypes() != null) {
      return !mode.getForbiddenExtendedTypes().contains(gtfsExtendedType);
    }
    return true;
  }
}
