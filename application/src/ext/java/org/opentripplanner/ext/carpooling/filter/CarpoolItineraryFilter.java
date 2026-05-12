package org.opentripplanner.ext.carpooling.filter;

import java.time.Duration;
import org.opentripplanner.model.plan.Itinerary;

/**
 * Post-filter applied to fully-routed carpool itineraries with actual computed times.
 * <p>
 * Unlike {@link CarpoolTripFilter}, which screens raw trip candidates before routing using
 * estimated times to limit computational cost, implementations receive complete
 * {@link Itinerary} objects with actual departure and arrival times. This filter limits the
 * suggestions returned to the passenger to those that are valid according to their requested
 * trip — enforcing exact time constraints that pre-filters can only approximate.
 */
public interface CarpoolItineraryFilter {
  /**
   * Returns {@code true} if the itinerary satisfies this filter's criteria.
   *
   * @param itinerary    the routed itinerary to evaluate
   * @param request      the passenger's journey preferences
   * @param searchWindow the routing search window; may be {@code null}
   * @return {@code true} if the itinerary passes the filter, {@code false} otherwise
   */
  boolean isValidItinerary(Itinerary itinerary, CarpoolingRequest request, Duration searchWindow);
}
