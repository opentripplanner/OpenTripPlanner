package org.opentripplanner.ext.carpooling.filter;

import org.opentripplanner.ext.carpooling.model.CarpoolTrip;
import org.opentripplanner.framework.geometry.WgsCoordinate;

/**
 * Interface for filtering carpool trips before expensive routing calculations.
 * <p>
 * Filters are applied as a pre-screening mechanism to quickly eliminate
 * incompatible trips based on various criteria (direction, capacity, etc.).
 */
@FunctionalInterface
public interface TripFilter {
  /**
   * Checks if a trip passes this filter for the given passenger request.
   *
   * @param trip The carpool trip to evaluate
   * @param passengerPickup Passenger's pickup location
   * @param passengerDropoff Passenger's dropoff location
   * @return true if the trip passes the filter, false otherwise
   */
  boolean accepts(CarpoolTrip trip, WgsCoordinate passengerPickup, WgsCoordinate passengerDropoff);

  /**
   * Returns a filter that always accepts all trips.
   */
  static TripFilter acceptAll() {
    return (trip, pickup, dropoff) -> true;
  }

  /**
   * Returns a filter that combines this filter with another using AND logic.
   */
  default TripFilter and(TripFilter other) {
    return (trip, pickup, dropoff) ->
      this.accepts(trip, pickup, dropoff) && other.accepts(trip, pickup, dropoff);
  }

  /**
   * Returns a filter that combines this filter with another using OR logic.
   */
  default TripFilter or(TripFilter other) {
    return (trip, pickup, dropoff) ->
      this.accepts(trip, pickup, dropoff) || other.accepts(trip, pickup, dropoff);
  }

  /**
   * Returns a filter that negates this filter.
   */
  default TripFilter negate() {
    return (trip, pickup, dropoff) -> !this.accepts(trip, pickup, dropoff);
  }
}
