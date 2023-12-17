package org.opentripplanner.model.plan.paging.cursor;

import java.time.Instant;
import org.opentripplanner.model.plan.ItinerarySortKey;

/**
 * This class holds information needed to create the next/previous page cursors when there were
 * itineraries removed due to cropping the list of itineraries using the numItineraries parameter.
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
  Instant latestRemovedDeparture();

  /**
   * In case the result has too many results: The {@code numberOfItineraries} request parameter
   * is less than the number of itineraries found, then we keep the last itinerary kept and
   * returned as part of the result. The sort vector will be included in the page-cursor and
   * used in the next/previous page to filter away duplicates.
   */
  ItinerarySortKey pageCut();
}
