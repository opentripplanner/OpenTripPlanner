package org.opentripplanner.routing.api.request;

import org.opentripplanner.model.TransitMode;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class RequestModes {

  public StreetMode accessMode;
  public StreetMode egressMode;
  public StreetMode directMode;
  public Set<TransitMode> transitModes;

  public static RequestModes defaultRequestModes = new RequestModes(
      StreetMode.WALK,
      StreetMode.WALK,
      StreetMode.WALK,
      new HashSet<>(Arrays.asList(TransitMode.values()))
  );

  public RequestModes(
      StreetMode accessMode,
      StreetMode egressMode,
      StreetMode directMode,
      Set<TransitMode> transitModes
  ) {
    this.accessMode = (accessMode != null && accessMode.access) ? accessMode : null;
    this.egressMode = (egressMode != null && egressMode.egress) ? egressMode : null;
    this.directMode = directMode;
    this.transitModes = transitModes;
  }

  public boolean contains(StreetMode streetMode) {
    return
        streetMode.equals(accessMode)
            || streetMode.equals(egressMode)
            || streetMode.equals(directMode);
  }
}
