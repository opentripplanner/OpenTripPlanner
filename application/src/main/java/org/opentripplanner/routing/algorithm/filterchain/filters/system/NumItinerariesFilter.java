package org.opentripplanner.routing.algorithm.filterchain.filters.system;

import java.time.Instant;
import java.util.List;
import java.util.function.Consumer;
import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.model.plan.ItinerarySortKey;
import org.opentripplanner.routing.algorithm.filterchain.framework.spi.RemoveItineraryFlagger;
import org.opentripplanner.utils.collection.ListSection;
import org.opentripplanner.utils.collection.ListUtils;

/**
 * Flag all itineraries after the provided limit. This flags the itineraries at the end of the list
 * for removal, so the list should be sorted on the desired key before this filter is applied.
 * <p>
 * This filter reports information about the removed itineraries in the results consumer.
 */
public class NumItinerariesFilter implements RemoveItineraryFlagger {

  public static final String TAG = "number-of-itineraries-filter";

  private final int maxLimit;
  private final ListSection cropSection;
  private final Consumer<NumItinerariesFilterResults> numItinerariesFilterResultsSubscriber;

  public NumItinerariesFilter(
    int maxLimit,
    ListSection cropSection,
    Consumer<NumItinerariesFilterResults> numItinerariesFilterResultsSubscriber
  ) {
    if (numItinerariesFilterResultsSubscriber == null) {
      throw new IllegalArgumentException(
        "'numItinerariesFilterResultsSubscriber' should not be null"
      );
    }
    this.maxLimit = maxLimit;
    this.cropSection = cropSection;
    this.numItinerariesFilterResultsSubscriber = numItinerariesFilterResultsSubscriber;
  }

  @Override
  public String name() {
    return TAG;
  }

  @Override
  public List<Itinerary> flagForRemoval(List<Itinerary> itineraries) {
    if (itineraries.size() <= maxLimit) {
      return List.of();
    }

    List<Itinerary> itinerariesToKeep;
    List<Itinerary> itinerariesToRemove;

    if (cropSection == ListSection.HEAD) {
      int limit = itineraries.size() - maxLimit;

      itinerariesToRemove = itineraries.subList(0, limit);
      itinerariesToKeep = itineraries.subList(limit, itineraries.size());
    } else {
      itinerariesToRemove = itineraries.subList(maxLimit, itineraries.size());
      itinerariesToKeep = itineraries.subList(0, maxLimit);
    }

    List<Instant> removedDepartures = itinerariesToRemove
      .stream()
      .map(it -> it.startTime().toInstant())
      .toList();

    Instant earliestRemovedDeparture = removedDepartures
      .stream()
      .min(Instant::compareTo)
      .orElse(null);
    Instant latestRemovedDeparture = removedDepartures
      .stream()
      .max(Instant::compareTo)
      .orElse(null);
    ItinerarySortKey pageCut = null;

    if (cropSection == ListSection.HEAD) {
      pageCut = ListUtils.first(itinerariesToKeep);
    } else {
      pageCut = ListUtils.last(itinerariesToKeep);
    }

    numItinerariesFilterResultsSubscriber.accept(
      new NumItinerariesFilterResults(earliestRemovedDeparture, latestRemovedDeparture, pageCut)
    );

    return itinerariesToRemove;
  }
}
