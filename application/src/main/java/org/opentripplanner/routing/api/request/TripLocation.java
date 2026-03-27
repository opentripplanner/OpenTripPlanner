package org.opentripplanner.routing.api.request;

import java.time.Instant;
import java.util.Objects;
import javax.annotation.Nullable;
import org.opentripplanner.core.model.id.FeedScopedId;

/**
 * Identifies a position on-board a specific transit trip. Used to start (or potentially end) a trip
 * planning search from on-board a vehicle.
 * <p>
 * The stop location is always identified by its {@code stopLocationId}. A stop location can be a
 * quay or a stop place. Optionally, an
 * {@code aimedDepartureTime} can be provided to disambiguate when the same stop is visited
 * multiple times in the pattern (e.g. ring lines).
 *
 * @param tripOnDateReference Identifies the trip and service date, either by trip ID +
 *                            service date or by a trip-on-service-date ID.
 * @param stopLocationId      The stop location at which the traveler is considered to be
 *                            boarding. Used together with the trip to identify the exact stop
 *                            position in the pattern.
 * @param aimedDepartureTime  The aimed departure time at this stop. Used for disambiguation
 *                            when the stop appears more than once in the pattern. May be null
 *                            if not needed.
 */
public record TripLocation(
  TripOnDateReference tripOnDateReference,
  FeedScopedId stopLocationId,
  @Nullable Instant aimedDepartureTime
) {
  public TripLocation {
    Objects.requireNonNull(tripOnDateReference, "tripOnDateReference must be set");
    Objects.requireNonNull(stopLocationId, "stopLocationId must be set");
  }

  /**
   * Create a TripLocation identified by stop location ID only.
   */
  public static TripLocation of(
    TripOnDateReference tripOnDateReference,
    FeedScopedId stopLocationId
  ) {
    return new TripLocation(tripOnDateReference, stopLocationId, null);
  }

  /**
   * Create a TripLocation identified by stop location ID and aimed departure time.
   */
  public static TripLocation of(
    TripOnDateReference tripOnDateReference,
    FeedScopedId stopLocationId,
    Instant aimedDepartureTime
  ) {
    return new TripLocation(tripOnDateReference, stopLocationId, aimedDepartureTime);
  }
}
