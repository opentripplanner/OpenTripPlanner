package org.opentripplanner.routing.algorithm.filterchain;

import org.opentripplanner.model.plan.Itinerary;

import java.util.List;

/**
 * Filter or decorate itineraries.
 */
public interface ItineraryFilter {

    /**
     * A name used for debugging filters to attach to elements deleted by the filter.
     */
    String name();

    /**
     * Process the given itineraries returning the result.
     */
    List<Itinerary> filter(List<Itinerary> itineraries);
}
