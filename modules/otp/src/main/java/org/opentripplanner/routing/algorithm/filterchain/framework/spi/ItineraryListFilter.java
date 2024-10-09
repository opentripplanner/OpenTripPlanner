package org.opentripplanner.routing.algorithm.filterchain.framework.spi;

import java.util.List;
import org.opentripplanner.model.plan.Itinerary;

/**
 * Filter, sort or decorate itineraries. A filter can modify the elements in the list, but not the
 * List. It should treat the list as immutable. Do not change the list passed into the filter,
 * instead　make a copy, change it and return the copy. It is allowed to return the list unchanged.
 * <p>
 * A filter should do only one thing! For example, do not change the itineraries and delete elements
 * in the same filter. Instead, create two filters and insert them after each other in the filter
 * chain.
 * <p>
 * This allows decoration of each filter and makes it easier to reuse logic. Like the
 * {@link org.opentripplanner.routing.algorithm.filterchain.framework.filter.MaxLimit} is reused
 * in several places.
 */
public interface ItineraryListFilter {
  /**
   * Process the given itineraries returning the result.
   * <p>
   * This function should not change the List instance passed in, but may change the elements. It
   * must return a List with all the elements passed in (and possibly new elements). Note! The
   * list passed into the filter might be immutable.
   * <p>
   * This can be achieved using streams. Example:
   * <pre>
   * return itineraries.stream().peek(...).toList();
   * </pre>
   */
  List<Itinerary> filter(List<Itinerary> itineraries);
}
