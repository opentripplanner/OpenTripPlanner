package org.opentripplanner.routing.algorithm.filterchain.deletionflagger;

import java.time.Instant;
import java.util.List;
import org.opentripplanner.framework.collection.ListSection;
import org.opentripplanner.framework.collection.ListUtils;
import org.opentripplanner.framework.tostring.ToStringBuilder;
import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.model.plan.ItinerarySortKey;
import org.opentripplanner.model.plan.paging.cursor.PageCursorInput;

public class NumItinerariesFilterResults implements PageCursorInput {

  private final Instant earliestRemovedDeparture;
  private final Instant latestRemovedDeparture;
  private final Instant latestRemovedArrival;
  private final Instant earliestKeptArrival;
  private final ItinerarySortKey pageCut;
  private final ListSection cropSection;

  /**
   * The NumItinerariesFilter removes itineraries from a list of itineraries based on the number to
   * keep and whether it should crop at the head or the tail of the list. The results class keeps
   * the extreme endpoints of the sets of itineraries that were kept and removed, as well as more
   * details about the first itinerary removed (bottom of the head, or top of the tail) and whether
   * itineraries were cropped at the head or the tail.
   */
  public NumItinerariesFilterResults(
    List<Itinerary> keptItineraries,
    List<Itinerary> removedItineraries,
    ListSection cropSection
  ) {
    List<Instant> removedDepartures = removedItineraries
      .stream()
      .map(it -> it.startTime().toInstant())
      .toList();
    List<Instant> removedArrivals = removedItineraries
      .stream()
      .map(it -> it.endTime().toInstant())
      .toList();
    this.earliestRemovedDeparture = removedDepartures.stream().min(Instant::compareTo).orElse(null);
    this.latestRemovedDeparture = removedDepartures.stream().max(Instant::compareTo).orElse(null);
    this.latestRemovedArrival = removedArrivals.stream().max(Instant::compareTo).orElse(null);

    this.earliestKeptArrival =
      keptItineraries
        .stream()
        .map(it -> it.endTime().toInstant())
        .min(Instant::compareTo)
        .orElseThrow();

    if (cropSection == ListSection.HEAD) {
      pageCut = ListUtils.last(removedItineraries);
    } else {
      pageCut = ListUtils.first(removedItineraries);
    }
    this.cropSection = cropSection;
  }

  @Override
  public Instant earliestRemovedDeparture() {
    return earliestRemovedDeparture;
  }

  @Override
  public Instant earliestKeptArrival() {
    return earliestKeptArrival;
  }

  @Override
  public Instant latestRemovedDeparture() {
    return latestRemovedDeparture;
  }

  @Override
  public Instant latestRemovedArrival() {
    return latestRemovedArrival;
  }

  @Override
  public ItinerarySortKey pageCut() {
    return pageCut;
  }

  @Override
  public String toString() {
    return ToStringBuilder
      .of(NumItinerariesFilterResults.class)
      .addDateTime("earliestRemovedDeparture", earliestRemovedDeparture)
      .addDateTime("latestRemovedDeparture", latestRemovedDeparture)
      .addDateTime("latestRemovedArrival", latestRemovedArrival)
      .addDateTime("earliestKeptArrival", earliestKeptArrival)
      .addObjOp("pageCut", pageCut, ItinerarySortKey::keyAsString)
      .addEnum("cropSection", cropSection)
      .toString();
  }
}
