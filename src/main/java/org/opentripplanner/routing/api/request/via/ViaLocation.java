package org.opentripplanner.routing.api.request.via;

import java.time.Duration;
import java.util.List;
import javax.annotation.Nullable;
import org.opentripplanner.framework.geometry.WgsCoordinate;
import org.opentripplanner.transit.model.framework.FeedScopedId;

/**
 * Defines a via location which the journey must route through. At least one stop location or
 * coordinate must exist. When routing, the via-location is visited if at least one of the stops
 * or coordinates is visited, before the journey continues. There is no need to visit any other
 * stop location or coordinate.
 * <p>
 * The stop locations and coordinates are distinct locations. In earlier versions of OTP the
 * coordinates were used as a fallback for when a stop was not found. But in this version, a
 * {@link org.opentripplanner.transit.model.framework.EntityNotFoundException} is thrown if
 * one of the stops does not exist. The search does NOT try to be smart and recover from an
 * entity not found exception.
 */
public interface ViaLocation {
  /**
   * Get an optional name/label of for debugging and logging. Not used in business logic.
   */
  @Nullable
  String label();

  /**
   * The minimum wait time is used to force the trip to stay the given duration at the via location
   * before the trip is continued. This cannot be used together with allow-pass-through, since a
   * pass-through stop is visited on-board.
   */
  default Duration minimumWaitTime() {
    return Duration.ZERO;
  }

  /**
   * Returns {@code true} if this location is a pass-through-point. Only stops can be visited and
   * the {@code minimumWaitTime} must be zero.
   */
  boolean isPassThroughLocation();

  /**
   * A list of stops which can be used as via location together with the {@code coordinates}. A stop
   * location can be a stop, a station, a multimodal station or a group of stations.
   */
  List<FeedScopedId> stopLocationIds();

  /**
   * A list of coordinates used together with the {@code stopLocationIds} as the via location.
   * This is optional, an empty list is returned if no coordinates are available.
   */
  default List<WgsCoordinate> coordinates() {
    return List.of();
  }
}
