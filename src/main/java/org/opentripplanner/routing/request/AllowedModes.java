package org.opentripplanner.routing.request;

import java.util.Set;

public class AllowedModes {
  public final StreetMode accessMode;
  public final StreetMode egressMode;
  public final StreetMode directMode;
  public final Set<TransitMode> transitModes;

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
