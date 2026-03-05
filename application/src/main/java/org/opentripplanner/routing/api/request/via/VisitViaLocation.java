package org.opentripplanner.routing.api.request.via;

import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import javax.annotation.Nullable;
import org.opentripplanner.core.model.id.FeedScopedId;
import org.opentripplanner.model.GenericLocation;
import org.opentripplanner.street.geometry.WgsCoordinate;
import org.opentripplanner.utils.time.DurationUtils;
import org.opentripplanner.utils.tostring.ToStringBuilder;

/**
 * A visit-via-location is a physical visit to one of the stops or coordinates listed. An on-board
 * visit does not count. The traveler must alight or board at the given stop for it to to be
 * accepted. To visit a coordinate, the traveler must walk(bike or drive) to the closest point in
 * the street network from a stop and back to another stop to join the transit network.
 */
public class VisitViaLocation extends AbstractViaLocation {

  private static final Duration MINIMUM_WAIT_TIME_MAX_LIMIT = Duration.ofHours(24);

  @Nullable
  private final Duration minimumWaitTime;

  @Nullable
  private final WgsCoordinate coordinate;

  public VisitViaLocation(
    @Nullable String label,
    @Nullable Duration minimumWaitTime,
    List<FeedScopedId> stopLocationIds,
    @Nullable WgsCoordinate coordinate
  ) {
    super(label, stopLocationIds);
    this.minimumWaitTime = DurationUtils.requireNonNegative(
      minimumWaitTime == null ? Duration.ZERO : minimumWaitTime,
      MINIMUM_WAIT_TIME_MAX_LIMIT,
      "minimumWaitTime"
    );
    this.coordinate = coordinate;

    if (stopLocationIds().isEmpty() && this.coordinate == null) {
      throw new IllegalArgumentException(
        "A via location must have at least one stop location or a coordinate." +
          (label == null ? "" : " Label: " + label)
      );
    }
  }

  /**
   * Returns the location's coordinate as a {@link GenericLocation}. {@code null} is returned if the
   * location has only stop locations.
   */
  @Nullable
  public GenericLocation coordinateLocation() {
    if (coordinate == null) {
      return null;
    }
    return new GenericLocation(label(), null, coordinate.latitude(), coordinate.longitude());
  }

  /**
   * The minimum wait time is used to force the trip to stay the given duration at the via location
   * before the trip is continued. This cannot be used together with allow-pass-through, since a
   * pass-through stop is visited on-board.
   */
  @Override
  public Duration minimumWaitTime() {
    return minimumWaitTime;
  }

  @Override
  public boolean isPassThroughLocation() {
    return false;
  }

  @Override
  public Optional<WgsCoordinate> coordinate() {
    return Optional.ofNullable(coordinate);
  }

  @Override
  public String toString() {
    return ToStringBuilder.of(VisitViaLocation.class)
      .addObj("label", label())
      .addDuration("minimumWaitTime", minimumWaitTime, Duration.ZERO)
      .addCol("stopLocationIds", stopLocationIds())
      .addObj("coordinate", coordinate)
      .toString();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    if (!super.equals(o)) {
      return false;
    }
    VisitViaLocation that = (VisitViaLocation) o;
    return (
      Objects.equals(minimumWaitTime, that.minimumWaitTime) &&
      Objects.equals(coordinate, that.coordinate)
    );
  }

  @Override
  public int hashCode() {
    return Objects.hash(super.hashCode(), minimumWaitTime, coordinate);
  }
}
