package org.opentripplanner.routing.algorithm.filterchain.filters.system;

import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.routing.algorithm.filterchain.framework.spi.RemoveItineraryFlagger;
import org.opentripplanner.utils.collection.ListSection;

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
  private NumItinerariesFilterResult numItinerariesFilterResult = null;
  private final Consumer<NumItinerariesFilterResult> numItinerariesFilterResultSubscriber;

  public NumItinerariesFilter(
    int maxLimit,
    ListSection cropSection,
    Consumer<NumItinerariesFilterResult> numItinerariesFilterResultSubscriber
  ) {
    Objects.requireNonNull(
      numItinerariesFilterResultSubscriber,
      "'numItinerariesFilterResultSubscriber' should not be null"
    );
    this.maxLimit = maxLimit;
    this.cropSection = cropSection;
    this.numItinerariesFilterResultSubscriber = numItinerariesFilterResultSubscriber;
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

    // This result is used for paging. It is collected by an aggregator.
    numItinerariesFilterResult = new NumItinerariesFilterResult(itinerariesToKeep, itinerariesToRemove, cropSection);

    return itinerariesToRemove;
  }

  public NumItinerariesFilterResult getNumItinerariesFilterResult() {
    return numItinerariesFilterResult;
  }
}
