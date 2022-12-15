package org.opentripplanner.netex.mapping.support;

import org.opentripplanner.transit.model.basic.TransitMode;

/** A tuple of main- and sub-mode for use in netex import only */
public record NetexMainAndSubMode(TransitMode mainMode, String subMode) {
  public NetexMainAndSubMode(TransitMode mainMode) {
    this(mainMode, null);
  }
}
