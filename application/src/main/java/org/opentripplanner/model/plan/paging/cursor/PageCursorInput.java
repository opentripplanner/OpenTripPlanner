package org.opentripplanner.model.plan.paging.cursor;

import java.util.OptionalInt;
import org.opentripplanner.routing.algorithm.filterchain.filters.system.NumItinerariesFilterResults;

/**
 * This class holds information needed to create the next/previous page cursors either when there were
 * itineraries removed due to cropping the list of itineraries using the numItineraries parameter or
 * when the RemoveTransitIfStreetOnlyIsBetter is used.
 * <p>
 * The Instant fields come from the sets of itineraries that were removed and the ones that were
 * kept as a result of using the numItineraries parameter.
 * The streetOnlyCost is the cost of the best street only itinerary that was found in the first search.
 */
public interface PageCursorInput {
  NumItinerariesFilterResults numItinerariesFilterResults();
  /**
   * The best street only cost comes from taking the cost of the best street only itinerary from the first search.
   * This is used as a comparison in RemoveTransitIfStreetOnlyIsBetter when paging is used.
   */
  OptionalInt bestStreetOnlyCost();
}
