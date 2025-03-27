package org.opentripplanner.model.plan.paging.cursor;

import org.opentripplanner.routing.algorithm.filterchain.filters.system.NumItinerariesFilterResult;
import org.opentripplanner.routing.algorithm.filterchain.filters.transit.RemoveTransitIfStreetOnlyIsBetterResult;

/**
 * This class holds information needed to create the next/previous page cursors either when there were
 * itineraries removed due to cropping the list of itineraries using the numItineraries parameter or
 * when the {@link org.opentripplanner.routing.algorithm.filterchain.filters.transit.RemoveTransitIfStreetOnlyIsBetter}
 * filter is used.
 */
public interface PageCursorInput {
  /**
   * The itinerary filter chain may crop the search window as a result of reducing the list down to a specified maximum number of itineraries.
   * This contains the results from {@link org.opentripplanner.routing.algorithm.filterchain.filters.system.NumItinerariesFilter}.
   */
  NumItinerariesFilterResult numItinerariesFilterResult();
  /**
   * {@link RemoveTransitIfStreetOnlyIsBetterResult} contains a maximum cost for itineraries.
   * It is calculated by taking the cost of the best street only itinerary from the first search.
   * This is used as a comparison in {@link org.opentripplanner.routing.algorithm.filterchain.filters.transit.RemoveTransitIfStreetOnlyIsBetter}
   * when paging is used.
   */
  RemoveTransitIfStreetOnlyIsBetterResult removeTransitIfStreetOnlyIsBetterResult();
}
