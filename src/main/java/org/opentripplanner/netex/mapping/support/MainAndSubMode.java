package org.opentripplanner.netex.mapping.support;

import org.opentripplanner.transit.model.network.TransitMode;

/** A tupple of main- and sub-mode for use in netex import only */
public record MainAndSubMode(TransitMode mainMode, String subMode) {
  public MainAndSubMode(TransitMode mainMode) {
    this(mainMode, null);
  }
}
