package org.opentripplanner.routing.api.request.via;

import java.time.Duration;
import java.util.List;
import java.util.Objects;
import javax.annotation.Nullable;
import org.opentripplanner.framework.geometry.WgsCoordinate;
import org.opentripplanner.transit.model.framework.FeedScopedId;
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

  private final Duration minimumWaitTime;
  private final List<WgsCoordinate> coordinates;

  public VisitViaLocation(
    @Nullable String label,
    @Nullable Duration minimumWaitTime,
    List<FeedScopedId> stopLocationIds,
    List<WgsCoordinate> coordinates
  ) {
    super(label, stopLocationIds);
    this.minimumWaitTime =
      DurationUtils.requireNonNegative(
        minimumWaitTime == null ? Duration.ZERO : minimumWaitTime,
        MINIMUM_WAIT_TIME_MAX_LIMIT,
        "minimumWaitTime"
      );
    this.coordinates = List.copyOf(coordinates);

    if (stopLocationIds().isEmpty() && coordinates().isEmpty()) {
      throw new IllegalArgumentException(
        "A via location must have at least one stop location or a coordinate." +
        (label == null ? "" : " Label: " + label)
      );
    }
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
  public List<WgsCoordinate> coordinates() {
    return coordinates;
  }

  @Override
  public String toString() {
    return ToStringBuilder
      .of(VisitViaLocation.class)
      .addObj("label", label())
      .addDuration("minimumWaitTime", minimumWaitTime, Duration.ZERO)
      .addCol("stopLocationIds", stopLocationIds())
      .addObj("coordinates", coordinates)
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
      Objects.equals(coordinates, that.coordinates)
    );
  }

  @Override
  public int hashCode() {
    return Objects.hash(super.hashCode(), minimumWaitTime, coordinates);
  }
}
