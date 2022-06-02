package org.opentripplanner.routing.api.request;

import java.util.Set;
import javax.annotation.Nonnull;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.opentripplanner.model.modes.AllowTransitModeFilter;

public class RequestModes {

  public static RequestModes defaultRequestModes = new RequestModes(
    StreetMode.WALK,
    StreetMode.WALK,
    StreetMode.WALK,
    StreetMode.WALK,
    AllowTransitModeFilter.ofAllTransitModes()
  );

  @Nonnull
  public StreetMode accessMode;

  @Nonnull
  public StreetMode transferMode;

  @Nonnull
  public StreetMode egressMode;

  @Nonnull
  public StreetMode directMode;

  @Nonnull
  public Set<AllowTransitModeFilter> transitModeFilters;

  public RequestModes(
    StreetMode accessMode,
    StreetMode transferMode,
    StreetMode egressMode,
    StreetMode directMode,
    Set<AllowTransitModeFilter> transitModeFilters
  ) {
    this.accessMode = (accessMode != null && accessMode.access) ? accessMode : StreetMode.NOT_SET;
    this.transferMode =
      (transferMode != null && transferMode.transfer) ? transferMode : StreetMode.NOT_SET;
    this.egressMode = (egressMode != null && egressMode.egress) ? egressMode : StreetMode.NOT_SET;
    this.directMode = directMode != null ? directMode : StreetMode.NOT_SET;
    this.transitModeFilters = AllowTransitModeFilter.merge(transitModeFilters);
  }

  public boolean contains(StreetMode streetMode) {
    return (
      streetMode.equals(accessMode) ||
      streetMode.equals(egressMode) ||
      streetMode.equals(directMode)
    );
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    RequestModes that = (RequestModes) o;

    if (accessMode != that.accessMode) return false;
    if (egressMode != that.egressMode) return false;
    if (directMode != that.directMode) return false;
    return transitModeFilters.equals(that.transitModeFilters);
  }

  @Override
  public String toString() {
    return new ToStringBuilder(this)
      .append("accessMode", accessMode)
      .append("egressMode", egressMode)
      .append("directMode", directMode)
      .append("transitModes", transitModeFilters)
      .toString();
  }
}
