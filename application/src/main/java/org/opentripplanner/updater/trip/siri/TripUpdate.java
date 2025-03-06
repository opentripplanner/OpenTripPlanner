package org.opentripplanner.updater.trip.siri;

import java.time.LocalDate;
import java.util.Objects;
import javax.annotation.Nullable;
import org.opentripplanner.transit.model.network.StopPattern;
import org.opentripplanner.transit.model.network.TripPattern;
import org.opentripplanner.transit.model.timetable.RealTimeTripTimes;
import org.opentripplanner.transit.model.timetable.TripOnServiceDate;
import org.opentripplanner.transit.model.timetable.TripTimes;

/**
 * Represents the SIRI real-time update of a single trip.
 *
 * @param stopPattern            the stop pattern to which belongs the updated trip.
 * @param tripTimes              the new trip times for the updated trip.
 * @param serviceDate            the service date for which this update applies (updates are valid
 *                               only for one service date)
 * @param addedTripOnServiceDate optionally if this trip update adds a new trip, the
 *                               TripOnServiceDate corresponding to this new trip.
 * @param addedTripPattern       optionally if this trip update adds a new trip pattern , the new
 *                               trip pattern to this new trip.
 * @param routeCreation          true if an added trip cannot be registered under an existing route
 *                               and a new route must be created.
 * @param dataSource             the dataSource of the real-time update.
 */
record TripUpdate(
  StopPattern stopPattern,
  TripTimes tripTimes,
  LocalDate serviceDate,
  @Nullable TripOnServiceDate addedTripOnServiceDate,
  @Nullable TripPattern addedTripPattern,
  boolean routeCreation,
  @Nullable String dataSource
) {
  public TripUpdate {
    Objects.requireNonNull(stopPattern);
    Objects.requireNonNull(tripTimes);
    Objects.requireNonNull(serviceDate);
  }

  /**
   * Create a trip update for an existing trip.
   */
  public TripUpdate(
    StopPattern stopPattern,
    RealTimeTripTimes updatedTripTimes,
    LocalDate serviceDate,
    String dataSource
  ) {
    this(stopPattern, updatedTripTimes, serviceDate, null, null, false, dataSource);
  }

  /**
   * Return true if this trip update creates a new trip pattern.
   */
  public boolean tripPatternCreation() {
    return addedTripPattern != null;
  }

  /**
   * Return true if this trip update creates a new trip.
   */
  public boolean tripCreation() {
    return addedTripOnServiceDate != null;
  }
}
