package org.opentripplanner.routing.algorithm;

import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.routing.core.RoutingRequest;

import java.util.List;


/**
 * Filter or decorate itineraries.
 */
@FunctionalInterface
public interface ItineraryFilter {
    List<Itinerary> filter(RoutingRequest request, List<Itinerary> itineraries);
}
