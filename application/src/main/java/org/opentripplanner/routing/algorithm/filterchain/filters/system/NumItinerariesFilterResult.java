package org.opentripplanner.routing.algorithm.filterchain.filters.system;

import java.time.Instant;
import java.util.List;
import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.model.plan.ItinerarySortKey;
import org.opentripplanner.utils.collection.ListSection;
import org.opentripplanner.utils.collection.ListUtils;
import org.opentripplanner.utils.tostring.ToStringBuilder;

/**
 * The NumItinerariesFilter removes itineraries from a list of itineraries based on the number to
 * keep and whether it should crop at the head or the tail of the list. The results class keeps
 * the extreme endpoints of the sets of itineraries that were kept and removed, as well as more
 * details about the first itinerary removed (bottom of the head, or top of the tail) and whether
 * itineraries were cropped at the head or the tail.
 */
public class NumItinerariesFilterResult {

  private final Instant earliestRemovedDeparture;
  private final Instant latestRemovedDeparture;
  private final ItinerarySortKey pageCut;

  public NumItinerariesFilterResult(
    Instant earliestRemovedDeparture,
    Instant latestRemovedDeparture,
    ItinerarySortKey pageCut
  ) {
    this.earliestRemovedDeparture = earliestRemovedDeparture;
    this.latestRemovedDeparture = latestRemovedDeparture;
    this.pageCut = pageCut;
  }

  public NumItinerariesFilterResult(
    List<Itinerary> keptItineraries,
    List<Itinerary> removedItineraries,
    ListSection cropSection
  ) {
    List<Instant> removedDepartures = removedItineraries
      .stream()
      .map(it -> it.startTime().toInstant())
      .toList();
    this.earliestRemovedDeparture = removedDepartures.stream().min(Instant::compareTo).orElse(null);
    this.latestRemovedDeparture = removedDepartures.stream().max(Instant::compareTo).orElse(null);

    if (cropSection == ListSection.HEAD) {
      pageCut = ListUtils.first(keptItineraries);
    } else {
      pageCut = ListUtils.last(keptItineraries);
    }
  }

  /**
   * The earliest removed departure defines the start of the search window following the
   * current window. To include this removed itinerary (and all other removed itineraries)
   * in the next page search the search windows must overlap.
   */
  public Instant earliestRemovedDeparture() {
    return earliestRemovedDeparture;
  }

  /**
   * The latest removed departure together with a search window duration is used to
   * calculate the start of the search window preceding the current window.
   */
  public Instant latestRemovedDeparture() {
    return latestRemovedDeparture;
  }

  /**
   * If the search has too many results, the {@code numberOfItineraries} request parameter
   * is less than the number of itineraries found, then we store information from the last
   * itinerary kept and returned as part of the result. The sort vector will be included
   * in the page cursor and used in the next/previous page to filter away duplicates.
   */
  public ItinerarySortKey pageCut() {
    return pageCut;
  }

  @Override
  public String toString() {
    return ToStringBuilder.of(NumItinerariesFilterResult.class)
      .addDateTime("earliestRemovedDeparture", earliestRemovedDeparture)
      .addDateTime("latestRemovedDeparture", latestRemovedDeparture)
      .addObjOp("pageCut", pageCut, ItinerarySortKey::keyAsString)
      .toString();
  }
}
