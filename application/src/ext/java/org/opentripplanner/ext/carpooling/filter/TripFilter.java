package org.opentripplanner.ext.carpooling.filter;

import java.time.Instant;
import org.opentripplanner.ext.carpooling.model.CarpoolTrip;
import org.opentripplanner.framework.geometry.WgsCoordinate;

/**
 * Interface for filtering carpool trips before expensive routing calculations.
 * <p>
 * Filters are applied as a pre-screening mechanism to quickly eliminate
 * incompatible trips based on various criteria (direction, capacity, time, distance, etc.).
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
   * Checks if a trip passes this filter for the given passenger request with time information.
   * <p>
   * Default implementation delegates to the simpler {@link #accepts(CarpoolTrip, WgsCoordinate, WgsCoordinate)}
   * method, ignoring the time parameter. Time-aware filters should override this method.
   *
   * @param trip The carpool trip to evaluate
   * @param passengerPickup Passenger's pickup location
   * @param passengerDropoff Passenger's dropoff location
   * @param passengerDepartureTime Passenger's requested departure time
   * @return true if the trip passes the filter, false otherwise
   */
  default boolean accepts(
    CarpoolTrip trip,
    WgsCoordinate passengerPickup,
    WgsCoordinate passengerDropoff,
    Instant passengerDepartureTime
  ) {
    // Default: ignore time and delegate to coordinate-only method
    return accepts(trip, passengerPickup, passengerDropoff);
  }

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
    return new TripFilter() {
      @Override
      public boolean accepts(CarpoolTrip trip, WgsCoordinate pickup, WgsCoordinate dropoff) {
        return (
          TripFilter.this.accepts(trip, pickup, dropoff) && other.accepts(trip, pickup, dropoff)
        );
      }

      @Override
      public boolean accepts(
        CarpoolTrip trip,
        WgsCoordinate pickup,
        WgsCoordinate dropoff,
        Instant time
      ) {
        return (
          TripFilter.this.accepts(trip, pickup, dropoff, time) &&
          other.accepts(trip, pickup, dropoff, time)
        );
      }
    };
  }

  /**
   * Returns a filter that combines this filter with another using OR logic.
   */
  default TripFilter or(TripFilter other) {
    return new TripFilter() {
      @Override
      public boolean accepts(CarpoolTrip trip, WgsCoordinate pickup, WgsCoordinate dropoff) {
        return (
          TripFilter.this.accepts(trip, pickup, dropoff) || other.accepts(trip, pickup, dropoff)
        );
      }

      @Override
      public boolean accepts(
        CarpoolTrip trip,
        WgsCoordinate pickup,
        WgsCoordinate dropoff,
        Instant time
      ) {
        return (
          TripFilter.this.accepts(trip, pickup, dropoff, time) ||
          other.accepts(trip, pickup, dropoff, time)
        );
      }
    };
  }

  /**
   * Returns a filter that negates this filter.
   */
  default TripFilter negate() {
    return new TripFilter() {
      @Override
      public boolean accepts(CarpoolTrip trip, WgsCoordinate pickup, WgsCoordinate dropoff) {
        return !TripFilter.this.accepts(trip, pickup, dropoff);
      }

      @Override
      public boolean accepts(
        CarpoolTrip trip,
        WgsCoordinate pickup,
        WgsCoordinate dropoff,
        Instant time
      ) {
        return !TripFilter.this.accepts(trip, pickup, dropoff, time);
      }
    };
  }
}
