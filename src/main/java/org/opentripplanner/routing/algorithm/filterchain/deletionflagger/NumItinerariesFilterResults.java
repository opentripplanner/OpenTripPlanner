package org.opentripplanner.routing.algorithm.filterchain.deletionflagger;

import java.time.Instant;
import java.util.List;
import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.routing.algorithm.filterchain.ListSection;

public class NumItinerariesFilterResults {

  public final Instant earliestRemovedDeparture;
  public final Instant latestRemovedDeparture;
  public final Instant earliestRemovedArrival;
  public final Instant latestRemovedArrival;
  public final Instant earliestKeptArrival;
  public final Instant firstRemovedArrivalTime;
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
        .orElse(null);

    Itinerary firstRemovedItinerary;
    if (cropSection == ListSection.HEAD) {
      firstRemovedItinerary = removedItineraries.get(removedItineraries.size() - 1);
    } else {
      firstRemovedItinerary = removedItineraries.get(0);
    }

    this.firstRemovedArrivalTime = firstRemovedItinerary.endTime().toInstant();
    this.firstRemovedGeneralizedCost = firstRemovedItinerary.getGeneralizedCost();
    this.firstRemovedNumOfTransfers = firstRemovedItinerary.getNumberOfTransfers();
    this.firstRemovedDepartureTime = firstRemovedItinerary.startTime().toInstant();

    this.cropSection = cropSection;
  }

  @Override
  public String toString() {
    return (
      "NumItinerariesFilterResults{" +
      "earliestRemovedDeparture=" +
      earliestRemovedDeparture +
      ", latestRemovedDeparture=" +
      latestRemovedDeparture +
      ", earliestRemovedArrival=" +
      earliestRemovedArrival +
      ", latestRemovedArrival=" +
      latestRemovedArrival +
      ", earliestKeptArrival=" +
      earliestKeptArrival +
      ", firstRemovedArrivalTime=" +
      firstRemovedArrivalTime +
      ", firstRemovedGeneralizedCost=" +
      firstRemovedGeneralizedCost +
      ", firstRemovedNumOfTransfers=" +
      firstRemovedNumOfTransfers +
      ", firstRemovedDepartureTime=" +
      firstRemovedDepartureTime +
      ", cropSection=" +
      cropSection +
      '}'
    );
  }
}
