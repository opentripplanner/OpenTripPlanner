package org.opentripplanner.routing.algorithm.filterchain.filters.system;

import java.util.List;
import java.util.function.Consumer;
import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.model.plan.paging.cursor.PageCursorInput;
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

  private static final Consumer<PageCursorInput> IGNORE_SUBSCRIBER = i -> {};

  private final int maxLimit;
  private final ListSection cropSection;
  private final Consumer<PageCursorInput> pageCursorInputSubscriber;

  public NumItinerariesFilter(
    int maxLimit,
    ListSection cropSection,
    Consumer<PageCursorInput> pageCursorInputSubscriber
  ) {
    this.maxLimit = maxLimit;
    this.cropSection = cropSection;
    this.pageCursorInputSubscriber = pageCursorInputSubscriber == null
      ? IGNORE_SUBSCRIBER
      : pageCursorInputSubscriber;
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

    pageCursorInputSubscriber.accept(
      new NumItinerariesFilterResults(itinerariesToKeep, itinerariesToRemove, cropSection)
    );

    return itinerariesToRemove;
  }
}
