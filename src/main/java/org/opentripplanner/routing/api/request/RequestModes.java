package org.opentripplanner.routing.api.request;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import javax.annotation.Nonnull;
import org.opentripplanner.transit.model.network.MainAndSubMode;
import org.opentripplanner.util.lang.ToStringBuilder;

public class RequestModes {

  /**
   * The RequestModes is mutable, so we need to keep a private default set of modes.
   */
  private static final RequestModes defaultRequestModes = new RequestModes(
    StreetMode.WALK,
    StreetMode.WALK,
    StreetMode.WALK,
    StreetMode.WALK,
    MainAndSubMode.all()
  );

  @Nonnull
  public StreetMode accessMode;

  @Nonnull
  public StreetMode egressMode;

  @Nonnull
  public StreetMode directMode;

  @Nonnull
  public StreetMode transferMode;

  @Nonnull
  public List<MainAndSubMode> transitModes;

  public RequestModes(
    StreetMode accessMode,
    StreetMode transferMode,
    StreetMode egressMode,
    StreetMode directMode,
    Collection<MainAndSubMode> transitModes
  ) {
    this.accessMode = (accessMode != null && accessMode.access) ? accessMode : StreetMode.NOT_SET;
    this.egressMode = (egressMode != null && egressMode.egress) ? egressMode : StreetMode.NOT_SET;
    this.directMode = directMode != null ? directMode : StreetMode.NOT_SET;
    this.transferMode =
      (transferMode != null && transferMode.transfer) ? transferMode : StreetMode.NOT_SET;
    this.transitModes = transitModes == null ? MainAndSubMode.all() : List.copyOf(transitModes);
  }

  public static RequestModes defaultRequestModes() {
    // Clone default since this class is mutable
    return new RequestModes(
      defaultRequestModes.accessMode,
      defaultRequestModes.transferMode,
      defaultRequestModes.egressMode,
      defaultRequestModes.directMode,
      defaultRequestModes.transitModes
    );
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

    if (accessMode != that.accessMode) {
      return false;
    }
    if (egressMode != that.egressMode) {
      return false;
    }
    if (directMode != that.directMode) {
      return false;
    }
    if (transferMode != that.transferMode) {
      return false;
    }
    return transitModes.equals(that.transitModes);
  }

  @Override
  public int hashCode() {
    return Objects.hash(accessMode, egressMode, directMode, transferMode, transitModes);
  }

  @Override
  public String toString() {
    return ToStringBuilder
      .of(RequestModes.class)
      .addEnum("accessMode", accessMode)
      .addEnum("egressMode", egressMode)
      .addEnum("directMode", directMode)
      .addEnum("transferMode", transferMode)
      .addCol("transitModes", transitModes)
      .toString();
  }
}
