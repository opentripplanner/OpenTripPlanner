package org.opentripplanner.routing.algorithm.filterchain;

import org.opentripplanner.model.plan.Itinerary;

import java.util.List;

/**
 * Filter or decorate itineraries. A filter can modify the elements in the list, but not the List. 
 * It should treat the list as immutable. Do not change the list passed into the filter, instead make a copy,
 * change it and return the copy. It is allowed to return the list unchanged.
 * <p>
 * A filter should do only one thing! For example do not change the itineraries and delete elements in
 * the same filter. Instead create two filters and insert them after each other in the filter chain.
 * This allows decoration of each filter and make it easier to reuse logic. The
 * {@link org.opentripplanner.routing.algorithm.filterchain.filters.MaxLimitFilter} work with
 * both a normal filter chain and the
 * {@link org.opentripplanner.routing.algorithm.filterchain.filters.DebugFilterChain}, and the
 * logic can be reused in several places. So, because of this, most filters can ignore the
 * debug-mode.
 */
public interface ItineraryFilter {

    /**
     * A name used for debugging the filter chain.
     * <p>
     * Use '-' so separate words like: {@code sort-on-duration-filter}
     */
    String name();

    /**
     * Process the given itineraries returning the result.
     * <p>
     * This function should not change the List instance passed in, but may change the elements. It
     * can return a List with a subset of the elements (or even different, new elements). Note! that
     * the list passed into the filter might be immutable.
     * <p>
     * This can be achieved using streams. Example:
     * <pre>
     * return itineraries.stream().filter(...).collect(Collectors.toList());
     * </pre>
     */
    List<Itinerary> filter(List<Itinerary> itineraries);


    /**
     * Return {@code true} if the filter removes itineraries from the input list
     * in the {@link #filter(List)} method, or {@code false} if no itineraries are
     * deleted.
     * <p>
     * The default implementation returns {@code false}, make sure to override this in every
     * filter that deletes itineraries from the given input list.
     */
    default boolean removeItineraries() {
        return false;
    }
}
