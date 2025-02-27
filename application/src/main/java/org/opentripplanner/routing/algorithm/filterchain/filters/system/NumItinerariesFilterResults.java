package org.opentripplanner.routing.algorithm.filterchain.filters.system;

import java.time.Instant;
import org.opentripplanner.model.plan.ItinerarySortKey;
import org.opentripplanner.utils.tostring.ToStringBuilder;

/**
 * The NumItinerariesFilter removes itineraries from a list of itineraries based on the number to
 * keep and whether it should crop at the head or the tail of the list. The results class keeps
 * the extreme endpoints of the sets of itineraries that were kept and removed, as well as more
 * details about the first itinerary removed (bottom of the head, or top of the tail) and whether
 * itineraries were cropped at the head or the tail.
 * <p>
 * The earliest-removed-departure defines the start of the search-window following the
 * current window. To include this removed itinerary (and all other removed itineraries)
 * in the next-page search the search windows must overlap.
 * <p>
 * In case the result has too many results: The {@code numberOfItineraries} request parameter
 * is less than the number of itineraries found, then we keep the last itinerary kept and
 * returned as part of the result. The sort vector will be included in the page-cursor and
 * used in the next/previous page to filter away duplicates.
 */
public record NumItinerariesFilterResults(
  Instant earliestRemovedDeparture,
  Instant latestRemovedDeparture,
  ItinerarySortKey pageCut
) {
  @Override
  public String toString() {
    return ToStringBuilder
      .of(NumItinerariesFilterResults.class)
      .addDateTime("earliestRemovedDeparture", earliestRemovedDeparture)
      .addDateTime("latestRemovedDeparture", latestRemovedDeparture)
      .addObjOp("pageCut", pageCut, ItinerarySortKey::keyAsString)
      .toString();
  }
}
