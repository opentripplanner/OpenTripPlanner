package org.opentripplanner.routing.algorithm.filterchain.deletionflagger;

import java.util.List;
import java.util.function.Consumer;
import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.routing.algorithm.filterchain.ListSection;

/**
 * Flag all itineraries after the provided limit. This flags the itineraries at the end of the list
 * for removal, so the list should be sorted on the desired key before this filter is applied.
 * <p>
 * This filter reports information about the removed itineraries in the results consumer.
 */
public class NumItinerariesFilter implements ItineraryDeletionFlagger {

  public static final String TAG = "number-of-itineraries-filter";

  private static final Consumer<NumItinerariesFilterResults> IGNORE_SUBSCRIBER = i -> {};

  private final int maxLimit;
  private final ListSection cropSection;
  private final Consumer<NumItinerariesFilterResults> numItinerariesFilterResultsConsumer;

  public NumItinerariesFilter(
    int maxLimit,
    ListSection cropSection,
    Consumer<NumItinerariesFilterResults> numItinerariesFilterResultsConsumer
  ) {
    this.maxLimit = maxLimit;
    this.cropSection = cropSection;
    this.numItinerariesFilterResultsConsumer =
      numItinerariesFilterResultsConsumer == null
        ? IGNORE_SUBSCRIBER
        : numItinerariesFilterResultsConsumer;
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

    numItinerariesFilterResultsConsumer.accept(
      new NumItinerariesFilterResults(itinerariesToKeep, itinerariesToRemove, cropSection)
    );

    return itinerariesToRemove;
  }
}
