package org.opentripplanner.routing.api.request;

import static org.opentripplanner.routing.api.request.StreetMode.NOT_SET;

import java.util.Objects;
import org.opentripplanner.utils.tostring.ToStringBuilder;

public class RequestModes {

  /**
   * The RequestModes is mutable, so we need to keep a private default set of modes.
   */
  private static final RequestModes DEFAULTS = new RequestModes(
    StreetMode.WALK,
    StreetMode.WALK,
    StreetMode.WALK,
    StreetMode.WALK
  );

  public final StreetMode accessMode;

  public final StreetMode egressMode;

  public final StreetMode directMode;

  public final StreetMode transferMode;

  private RequestModes(
    StreetMode accessMode,
    StreetMode egressMode,
    StreetMode directMode,
    StreetMode transferMode
  ) {
    this.accessMode = (accessMode != null && accessMode.accessAllowed()) ? accessMode : NOT_SET;
    this.egressMode = (egressMode != null && egressMode.egressAllowed()) ? egressMode : NOT_SET;
    this.directMode = directMode != null ? directMode : NOT_SET;
    this.transferMode = (transferMode != null && transferMode.transferAllowed())
      ? transferMode
      : NOT_SET;
  }

  public RequestModes(RequestModesBuilder builder) {
    this(builder.accessMode(), builder.egressMode(), builder.directMode(), builder.transferMode());
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

    return transferMode == that.transferMode;
  }

  @Override
  public int hashCode() {
    return Objects.hash(accessMode, egressMode, directMode, transferMode);
  }

  @Override
  public String toString() {
    return ToStringBuilder.of(RequestModes.class)
      .addEnum("accessMode", accessMode)
      .addEnum("egressMode", egressMode)
      .addEnum("directMode", directMode)
      .addEnum("transferMode", transferMode)
      .toString();
  }
}
