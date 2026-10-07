package org.opentripplanner.ext.carpooling.filter;

import org.opentripplanner.ext.carpooling.routing.RoutableCarpoolTrip;

/**
 * Pre-filter applied to carpool trip candidates before expensive routing calculations.
 * <p>
 * Filters are applied once the passenger has been snapped to the street network, as a
 * pre-screening mechanism that quickly eliminates incompatible trips and limits the computational
 * cost of routing. Implementations must be necessary conditions only: a filter may never reject a
 * trip the insertion evaluation, and for direct itineraries the post-filters, would accept. Tight
 * enforcement of actual times is delegated to post-filters on the complete itinerary.
 * <p>
 * Supports direct routing (pickup + dropoff) and access/egress routing (single passenger
 * coordinate near a transit stop). The routing mode is communicated via
 * {@link CarpoolingRequest#isAccessEgressRequest()} and {@link CarpoolingRequest#isAccessRequest()}.
 */
public interface CarpoolTripFilter {
  /**
   * Returns {@code true} if the trip is a viable candidate worth routing.
   * <p>
   * A necessary condition only, to limit computational cost; tight enforcement is done by
   * post-filters after routing. The routing mode (direct, access, or egress) is available via the
   * passenger's request.
   *
   * @param trip      the carpool trip to evaluate, with its corridor
   * @param passenger the passenger's request and snapped pickup and/or dropoff
   * @return {@code true} if the trip is a candidate, {@code false} otherwise
   */
  boolean isCandidateTrip(RoutableCarpoolTrip trip, SnappedPassenger passenger);
}
