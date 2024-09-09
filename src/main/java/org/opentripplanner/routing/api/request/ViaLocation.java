package org.opentripplanner.routing.api.request;

import java.time.Duration;
import java.util.List;
import java.util.Objects;
import javax.annotation.Nullable;
import org.opentripplanner.framework.time.DurationUtils;
import org.opentripplanner.framework.tostring.ToStringBuilder;
import org.opentripplanner.transit.model.site.StopLocation;

/**
 * Defines a via location which the journey must route through.
 * <p>
 * TODO: The list of stop location forces the client to look up the stops before creating
 *       the request. This duplicates logic in all places using this. Instead this should
 *       be replaced by passing in ids and the look up the location inside the router.
 *       To do this properly the routing service must handle lookup errors properly and
 *       pass the error information properly back to the caller/client.
 */
public class ViaLocation {

  private static final Duration MINIMUM_WAIT_TIME_MAX_LIMIT = Duration.ofHours(24);

  private final boolean allowAsPassThroughPoint;
  private final Duration minimumWaitTime;
  private final String label;
  private final List<StopLocation> locations;

  public ViaLocation(
    @Nullable String label,
    boolean allowAsPassThroughPoint,
    Duration minimumWaitTime,
    List<StopLocation> locations
  ) {
    this.label = label;
    this.allowAsPassThroughPoint = allowAsPassThroughPoint;
    this.minimumWaitTime =
      DurationUtils.requireNonNegative(
        minimumWaitTime,
        MINIMUM_WAIT_TIME_MAX_LIMIT,
        "minimumWaitTime"
      );
    this.locations = List.copyOf(locations);

    if (allowAsPassThroughPoint && !minimumWaitTime.isZero()) {
      throw new IllegalArgumentException(
        "AllowAsPassThroughPoint can not be used with minimumWaitTime for " + label + "."
      );
    }
  }

  public ViaLocation(@Nullable String label, List<StopLocation> locations) {
    this(label, true, Duration.ZERO, locations);
  }

  /**
   * If set to {@code true} this location can be visited as a pass-through-point. Only
   * collections of stops are supported, not coordinates. Also, the minWaitTime must be
   * zero(0).
   */
  public boolean allowAsPassThroughPoint() {
    return allowAsPassThroughPoint;
  }

  /**
   * The minimum wait time is used to force the trip to stay the given duration at the via
   * location before the trip is continued. This cannot be used together with allow-pass-through,
   * since a pass-through stop is visited on-board.
   */
  public Duration minimumWaitTime() {
    return minimumWaitTime;
  }

  /**
   * Get an optional name/label of for debugging and logging.
   */
  @Nullable
  public String label() {
    return label;
  }

  /**
   * Get the one or multiple stops of which only one is required to route through.
   */
  public List<StopLocation> locations() {
    return locations;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    ViaLocation that = (ViaLocation) o;
    return (
      allowAsPassThroughPoint == that.allowAsPassThroughPoint &&
      Objects.equals(minimumWaitTime, that.minimumWaitTime) &&
      Objects.equals(label, that.label) &&
      Objects.equals(locations, that.locations)
    );
  }

  @Override
  public int hashCode() {
    return Objects.hash(allowAsPassThroughPoint, minimumWaitTime, label, locations);
  }

  @Override
  public String toString() {
    return ToStringBuilder
      .of(ViaLocation.class)
      .addBoolIfTrue(label, label != null)
      .addBoolIfTrue("allowAsPassThroughPoint", allowAsPassThroughPoint)
      .addDuration("minimumWaitTime", minimumWaitTime, Duration.ZERO)
      .addCol("locations", locations)
      .toString();
  }
}
