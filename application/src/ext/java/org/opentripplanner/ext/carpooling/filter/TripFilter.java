package org.opentripplanner.ext.carpooling.filter;

import java.time.Duration;
import org.opentripplanner.ext.carpooling.model.CarpoolTrip;

/**
 * Interface for filtering carpool trips before expensive routing calculations.
 * <p>
 * Filters are applied as a pre-screening mechanism to quickly eliminate incompatible trips based on
 * various criteria (direction, capacity, time, distance, etc.).
 */
@FunctionalInterface
public interface TripFilter {
  /**
   * Checks if a trip passes this filter for the given passenger request.
   *
   * @param trip         The carpool trip to evaluate
   * @param request      Passenger's journey preferences
   * @param searchWindow Time window around the requested departure time
   * @return true if the trip passes the filter, false otherwise
   */
  boolean accepts(CarpoolTrip trip, CarpoolingRequest request, Duration searchWindow);
}
