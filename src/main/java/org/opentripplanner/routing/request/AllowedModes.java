package org.opentripplanner.routing.request;

import org.opentripplanner.model.TransitMode;

import java.util.Set;

public class AllowedModes {
  public StreetMode accessMode;
  public StreetMode egressMode;
  public StreetMode directMode;
  public Set<TransitMode> transitModes;

  public AllowedModes(
      StreetMode accessMode,
      StreetMode egressMode,
      StreetMode directMode,
      Set<TransitMode> transitModes
  ) {
    this.accessMode = accessMode;
    this.egressMode = egressMode;
    this.directMode = directMode;
    this.transitModes = transitModes;
  }
}
