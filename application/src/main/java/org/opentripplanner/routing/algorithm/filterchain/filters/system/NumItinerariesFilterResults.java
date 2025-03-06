package org.opentripplanner.routing.algorithm.filterchain.filters.system;

import java.time.Instant;
import java.util.List;
import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.model.plan.ItinerarySortKey;
import org.opentripplanner.model.plan.paging.cursor.PageCursorInput;
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
class NumItinerariesFilterResults implements PageCursorInput {

  private final Instant earliestRemovedDeparture;
  private final Instant latestRemovedDeparture;
  private final ItinerarySortKey pageCut;

  public NumItinerariesFilterResults(
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

  @Override
  public Instant earliestRemovedDeparture() {
    return earliestRemovedDeparture;
  }

  @Override
  public Instant latestRemovedDeparture() {
    return latestRemovedDeparture;
  }

  @Override
  public ItinerarySortKey pageCut() {
    return pageCut;
  }

  @Override
  public String toString() {
    return ToStringBuilder.of(NumItinerariesFilterResults.class)
      .addDateTime("earliestRemovedDeparture", earliestRemovedDeparture)
      .addDateTime("latestRemovedDeparture", latestRemovedDeparture)
      .addObjOp("pageCut", pageCut, ItinerarySortKey::keyAsString)
      .toString();
  }
}
