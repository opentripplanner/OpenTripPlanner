package org.opentripplanner.model;

import java.time.LocalDate;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.opentripplanner.transit.model.network.TripPattern;
import org.opentripplanner.transit.model.timetable.TripOnServiceDate;
import org.opentripplanner.transit.model.timetable.TripTimes;

/**
 * Represents the real-time update of a single trip.
 * @param pattern the pattern to which belongs the updated trip. This can be a new pattern created in real-time.
 * @param updatedTripTimes the new trip times for the updated trip.
 * @param serviceDate the service date for which this update applies (updates are valid only for one service date)
 * @param addedTripOnServiceDate optionally if this trip update adds a new trip, the TripOnServiceDate corresponding to this new trip.
 * @param isAddedTrip true if this update creates a new trip, not present in scheduled data.
 * @param isAddedRoute true if an added trip cannot be registered under an existing route and a new route must be created.
 */
public record RealtimeUpdate(
  @Nonnull TripPattern pattern,
  @Nonnull TripTimes updatedTripTimes,
  @Nonnull LocalDate serviceDate,
  @Nullable TripOnServiceDate addedTripOnServiceDate,
  boolean isAddedTrip,
  boolean isAddedRoute
) {
  /**
   * Create a real-time update for an existing trip.
   */
  public RealtimeUpdate(TripPattern pattern, TripTimes updatedTripTimes, LocalDate serviceDate) {
    this(pattern, updatedTripTimes, serviceDate, null, false, false);
  }
}
