package org.opentripplanner.ext.carpooling.filter;

import org.opentripplanner.ext.carpooling.corridor.RoutableCarpoolTrip;

/**
 * Pre-filter applied to the trips once the passenger is snapped to the street network, before the
 * insertion evaluation. A filter is a necessary condition only: it may never reject a trip that
 * could serve the passenger inside the passenger's window.
 */
public interface CarpoolTripFilter {
  /**
   * @param trip      the carpool trip to evaluate, with its corridor
   * @param passenger the passenger's request and snapped pickup and/or dropoff
   * @return {@code true} if the trip is a candidate worth evaluating
   */
  boolean isCandidateTrip(RoutableCarpoolTrip trip, SnappedPassenger passenger);
}
