package org.opentripplanner.routing.api.request;

import java.time.Duration;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import javax.annotation.Nullable;
import org.opentripplanner.framework.time.DurationUtils;
import org.opentripplanner.framework.tostring.ToStringBuilder;
import org.opentripplanner.transit.model.framework.FeedScopedId;

/**
 * Defines a via location which the journey must route through.
 */
public final class ViaLocation {

  private static final Duration MINIMUM_WAIT_TIME_MAX_LIMIT = Duration.ofHours(24);

  private final String label;
  private final boolean allowAsPassThroughPoint;
  private final Duration minimumWaitTime;
  private final List<ViaConnection> connections;

  @SuppressWarnings("DataFlowIssue")
  private ViaLocation(
    @Nullable String label,
    boolean allowAsPassThroughPoint,
    @Nullable Duration minimumWaitTime,
    Collection<ViaConnection> connections
  ) {
    this.label = label;
    this.allowAsPassThroughPoint = allowAsPassThroughPoint;
    this.minimumWaitTime =
      DurationUtils.requireNonNegative(
        minimumWaitTime == null ? Duration.ZERO : minimumWaitTime,
        MINIMUM_WAIT_TIME_MAX_LIMIT,
        "minimumWaitTime"
      );
    this.connections = List.copyOf(connections);

    if (allowAsPassThroughPoint && !minimumWaitTime.isZero()) {
      throw new IllegalArgumentException(
        "'allowAsPassThroughPoint' can not be used with minimumWaitTime for " + label + "."
      );
    }
    if (allowAsPassThroughPoint && connections.stream().anyMatch(ViaConnection::hasCoordinate)) {
      throw new IllegalArgumentException(
        "'allowAsPassThroughPoint' can not be used with coordinates for " + label + "."
      );
    }
  }

  /**
   * A pass-through-location instructs the router to visit the location either by boarding,
   * alighting or on-board a transit.
   * @param label The name/label for this location. This is used for debugging and logging and is pass-through information.
   * @param locationIds The ID for the stop, station or multimodal station or groupOfStopPlace.
   */
  public static ViaLocation passThroughLocation(
    @Nullable String label,
    List<FeedScopedId> locationIds
  ) {
    return new ViaLocation(label, true, Duration.ZERO, ViaConnection.connections(locationIds));
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
   * Get the one or multiple locations of which only one is required to route through.
   */
  public List<ViaConnection> connections() {
    return connections;
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
      Objects.equals(connections, that.connections)
    );
  }

  @Override
  public int hashCode() {
    return Objects.hash(allowAsPassThroughPoint, minimumWaitTime, label, connections);
  }

  @Override
  public String toString() {
    return ToStringBuilder
      .of(ViaLocation.class)
      .addBoolIfTrue(label, label != null)
      .addBoolIfTrue("allowAsPassThroughPoint", allowAsPassThroughPoint)
      .addDuration("minimumWaitTime", minimumWaitTime, Duration.ZERO)
      .addObj("connections", connections)
      .toString();
  }
}
