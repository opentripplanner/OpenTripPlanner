package org.opentripplanner.routing.algorithm.filterchain;

import org.opentripplanner.model.plan.Itinerary;

import java.util.List;

/**
 * Filter or decorate itineraries. A filter can modify the list passed into it and/or the elements
 * in the list, but should not delete elements from the list.
 * <p>
 * This function should not change the List instance passed in, but may change the elements in that
 * List, and may return a List with a subset of the elements (or even different, new elements).
 * This can be achieved using streams. Example:
 * <pre>
 * return itineraries.stream().filter(...).collect(Collectors.toList());
 * </pre>
 * <p>
 * A filter should do one thing! For example do not change the itineraries and delete elements in
 * the same filter. Instead create two filters and insert them after each other in the filter chain.
 * This allows decoration of each filter and make it easier to reuse logic. The
 * {@link org.opentripplanner.routing.algorithm.filterchain.filters.MaxLimitFilter} work with
 * both a normal filter chain and the
 * {@link org.opentripplanner.routing.algorithm.filterchain.filters.DebugFilterChain}, and the
 * logic can be reused in several places. So, because of this, most filters do not need to account
 * for being in debug-mode.
 */
public interface ItineraryFilter {

    /**
     * A name used for debugging the filter chain.
     *
     * Use '-' so separate words like: {@code sort-on-duration-filter}
     */
    String name();

    /**
     * Process the given itineraries returning the result.
     */
    List<Itinerary> filter(List<Itinerary> itineraries);
}
