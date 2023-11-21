package org.opentripplanner.routing.algorithm.filterchain.deletionflagger;

import java.time.Instant;
import java.util.List;
import org.opentripplanner.framework.tostring.ToStringBuilder;
import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.model.plan.pagecursor.PageCursorInput;
import org.opentripplanner.model.plan.pagecursor.PagingDeduplicationSection;
import org.opentripplanner.routing.algorithm.filterchain.ListSection;

public class NumItinerariesFilterResults implements PageCursorInput {

  public final Instant earliestRemovedDeparture;
  public final Instant latestRemovedDeparture;
  public final Instant earliestRemovedArrival;
  public final Instant latestRemovedArrival;
  public final Instant earliestKeptArrival;
  public final Instant firstRemovedArrivalTime;
  public final boolean firstRemovedIsOnStreetAllTheWay;
  public final int firstRemovedGeneralizedCost;
  public final int firstRemovedNumOfTransfers;
  public final Instant firstRemovedDepartureTime;
  public final ListSection cropSection;

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
    this.earliestRemovedArrival = removedArrivals.stream().min(Instant::compareTo).orElse(null);
    this.latestRemovedArrival = removedArrivals.stream().max(Instant::compareTo).orElse(null);

    this.earliestKeptArrival =
      keptItineraries
        .stream()
        .map(it -> it.endTime().toInstant())
        .min(Instant::compareTo)
        .orElseThrow();

    Itinerary firstRemovedItinerary;
    if (cropSection == ListSection.HEAD) {
      firstRemovedItinerary = removedItineraries.get(removedItineraries.size() - 1);
    } else {
      firstRemovedItinerary = removedItineraries.get(0);
    }

    this.firstRemovedIsOnStreetAllTheWay = firstRemovedItinerary.isOnStreetAllTheWay();
    this.firstRemovedArrivalTime = firstRemovedItinerary.endTime().toInstant();
    this.firstRemovedGeneralizedCost = firstRemovedItinerary.getGeneralizedCost();
    this.firstRemovedNumOfTransfers = firstRemovedItinerary.getNumberOfTransfers();
    this.firstRemovedDepartureTime = firstRemovedItinerary.startTime().toInstant();

    this.cropSection = cropSection;
  }

  @Override
  public String toString() {
    return ToStringBuilder
      .of(NumItinerariesFilterResults.class)
      .addDateTime("earliestRemovedDeparture", earliestRemovedDeparture)
      .addDateTime("latestRemovedDeparture", latestRemovedDeparture)
      .addDateTime("earliestRemovedArrival", earliestRemovedArrival)
      .addDateTime("latestRemovedArrival", latestRemovedArrival)
      .addDateTime("earliestKeptArrival", earliestKeptArrival)
      .addDateTime("firstRemovedArrivalTime", firstRemovedArrivalTime)
      .addNum("firstRemovedGeneralizedCost", firstRemovedGeneralizedCost)
      .addNum("firstRemovedNumOfTransfers", firstRemovedNumOfTransfers)
      .addDateTime("firstRemovedDepartureTime", firstRemovedDepartureTime)
      .addEnum("cropSection", cropSection)
      .toString();
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
  public Instant firstRemovedArrivalTime() {
    return firstRemovedArrivalTime;
  }

  @Override
  public boolean firstRemovedIsOnStreetAllTheWay() {
    return firstRemovedIsOnStreetAllTheWay;
  }

  @Override
  public int firstRemovedGeneralizedCost() {
    return firstRemovedGeneralizedCost;
  }

  @Override
  public int firstRemovedNumOfTransfers() {
    return firstRemovedNumOfTransfers;
  }

  @Override
  public Instant firstRemovedDepartureTime() {
    return firstRemovedDepartureTime;
  }

  @Override
  public PagingDeduplicationSection deduplicationSection() {
    return switch (cropSection) {
      case HEAD -> PagingDeduplicationSection.TAIL;
      case TAIL -> PagingDeduplicationSection.HEAD;
    };
  }
}
