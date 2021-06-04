package org.opentripplanner.routing.api.request;

import org.apache.commons.lang3.builder.ToStringBuilder;
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

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    RequestModes that = (RequestModes) o;

    if (accessMode != that.accessMode) return false;
    if (egressMode != that.egressMode) return false;
    if (directMode != that.directMode) return false;
    return transitModes != null ? transitModes.equals(that.transitModes) : that.transitModes == null;
  }

  @Override
  public String toString() {
    return new ToStringBuilder(this)
            .append("accessMode", accessMode)
            .append("egressMode", egressMode)
            .append("directMode", directMode)
            .append("transitModes", transitModes)
            .toString();
  }
}
