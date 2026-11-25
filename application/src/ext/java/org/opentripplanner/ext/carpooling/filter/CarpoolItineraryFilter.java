package org.opentripplanner.ext.carpooling.filter;

import org.opentripplanner.ext.carpooling.routing.CarpoolingRequest;
import org.opentripplanner.model.plan.Itinerary;

/**
 * Post-filter applied to routed carpool itineraries. It enforces exactly what a
 * {@link CarpoolTripFilter} could only bound before the itinerary's times were known.
 */
public interface CarpoolItineraryFilter {
  /** Returns {@code true} if the itinerary is valid for the passenger's request. */
  boolean isValidItinerary(Itinerary itinerary, CarpoolingRequest request);
}
