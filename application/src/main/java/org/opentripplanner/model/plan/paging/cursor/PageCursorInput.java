package org.opentripplanner.model.plan.paging.cursor;

import java.time.Instant;
import org.opentripplanner.framework.model.Cost;
import org.opentripplanner.model.plan.ItinerarySortKey;

/**
 * This class holds information needed to create the next/previous page-cursors either when there were
 * itineraries removed due to cropping the list of itineraries using the numItineraries parameter or
 * when itineraries are removed because there is a better direct result. Most direct searches can be
 * time-shifted so they are not repeated when paging.
 * <p>
 * The Instant fields come from the sets of itineraries that were removed and the ones that were
 * kept as a result of using the numItineraries parameter.
 */
public interface PageCursorInput {
  /**
   * The earliest-removed-departure defines the start of the search-window following the
   * current window. To include this removed itinerary (and all other removed itineraries)
   * in the next-page search the search windows must overlap.
   */
  Instant earliestRemovedDeparture();

  /**
   * The latest removed departure together with a search window duration is used to
   * calculate the start of the search window preceding the current window.
   */
  Instant latestRemovedDeparture();

  /**
   * If the search has too many results, the {@code numberOfItineraries} request parameter
   * is less than the number of itineraries found. In this case, we store information from the
   * last itinerary kept and returned as part of the result.
   */
  ItinerarySortKey pageCut();

  /**
   * Transit itineraries that have a higher generalized cost than the limit will
   * be filtered away in the {@link org.opentripplanner.routing.algorithm.filterchain.filters.transit.RemoveTransitIfStreetOnlyIsBetter}
   * filter.
   */
  Cost generalizedCostMaxLimit();
}
