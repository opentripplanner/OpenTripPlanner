package org.opentripplanner.routing.api.request;

import static org.opentripplanner.routing.api.request.StreetMode.NOT_SET;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import javax.annotation.Nonnull;
import org.opentripplanner.framework.tostring.ToStringBuilder;
import org.opentripplanner.transit.model.basic.MainAndSubMode;

public class RequestModes {

  /**
   * The RequestModes is mutable, so we need to keep a private default set of modes.
   */
  private static final RequestModes DEFAULTS = new RequestModes(
    StreetMode.WALK,
    StreetMode.WALK,
    StreetMode.WALK,
    StreetMode.WALK,
    MainAndSubMode.all()
  );

  @Nonnull
  public final StreetMode accessMode;

  @Nonnull
  public final StreetMode egressMode;

  @Nonnull
  public final StreetMode directMode;

  @Nonnull
  public final StreetMode transferMode;

  @Nonnull
  public final List<MainAndSubMode> transitModes;

  private RequestModes(
    StreetMode accessMode,
    StreetMode egressMode,
    StreetMode directMode,
    StreetMode transferMode,
    Collection<MainAndSubMode> transitModes
  ) {
    this.accessMode = (accessMode != null && accessMode.access) ? accessMode : NOT_SET;
    this.egressMode = (egressMode != null && egressMode.egress) ? egressMode : NOT_SET;
    this.directMode = directMode != null ? directMode : NOT_SET;
    this.transferMode = (transferMode != null && transferMode.transfer) ? transferMode : NOT_SET;
    this.transitModes = transitModes == null ? MainAndSubMode.all() : List.copyOf(transitModes);
  }

  public RequestModes(RequestModesBuilder builder) {
    this(
      builder.accessMode(),
      builder.egressMode(),
      builder.directMode(),
      builder.transferMode(),
      builder.transitModes()
    );
  }

  /**
   * Return a mode builder with the defaults set.
   */
  public static RequestModesBuilder of() {
    return DEFAULTS.copyOf();
  }

  public RequestModesBuilder copyOf() {
    return new RequestModesBuilder(this);
  }

  /**
   * Return the default set of modes with WALK for all street modes and all transit modes set.
   * Tip: Use the {@link #of()} to change the defaults.
   */
  public static RequestModes defaultRequestModes() {
    return DEFAULTS;
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
