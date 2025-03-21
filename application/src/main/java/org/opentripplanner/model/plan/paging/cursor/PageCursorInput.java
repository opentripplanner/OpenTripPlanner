package org.opentripplanner.model.plan.paging.cursor;

import org.opentripplanner.routing.algorithm.filterchain.filters.system.NumItinerariesFilterResults;
import org.opentripplanner.routing.algorithm.filterchain.filters.transit.RemoveTransitIfStreetOnlyIsBetterResults;

/**
 * This class holds information needed to create the next/previous page cursors either when there were
 * itineraries removed due to cropping the list of itineraries using the numItineraries parameter or
 * when the {@link org.opentripplanner.routing.algorithm.filterchain.filters.transit.RemoveTransitIfStreetOnlyIsBetter}
 * filter is used.
 */
public interface PageCursorInput {
  /**
   * This contains the results from {@link org.opentripplanner.routing.algorithm.filterchain.filters.system.NumItinerariesFilter}.
   * The Instant fields in NumItinerariesFilterResults come from the sets of itineraries that were removed and the ones that were
   * kept as a result of using the numItineraries parameter.
   */
  NumItinerariesFilterResults numItinerariesFilterResults();
  /**
   * RemoveTransitIfStreetOnlyIsBetterResults contains the best street only cost that comes from taking the cost of the best street only itinerary from the first search.
   * This is used as a comparison in {@link org.opentripplanner.routing.algorithm.filterchain.filters.transit.RemoveTransitIfStreetOnlyIsBetter}
   * when paging is used.
   */
  RemoveTransitIfStreetOnlyIsBetterResults removeTransitIfStreetOnlyIsBetterResults();
}
