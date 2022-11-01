package org.opentripplanner.routing.algorithm.filterchain.deletionflagger;

import java.util.List;
import java.util.function.Consumer;
import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.routing.algorithm.filterchain.ListSection;

/**
 * Flag all itineraries after the provided limit. This flags the itineraries at the end of the list
 * for removal, so the list should be sorted on the desired key before this filter is applied.
 * <p>
 * The filter can also report the first itinerary in the list it will flag. The subscriber is
 * optional.
 */
public class MaxLimitFilter implements ItineraryDeletionFlagger {

  private static final Consumer<Itinerary> IGNORE_SUBSCRIBER = i -> {};

  private final String name;
  private final int maxLimit;
  private final ListSection cropSection;
  private final Consumer<Itinerary> changedSubscriber;

  /** Create filter with default crop(TAIL) and without any callback. */
  public MaxLimitFilter(String name, int maxLimit) {
    this(name, maxLimit, ListSection.TAIL, IGNORE_SUBSCRIBER);
  }

  public MaxLimitFilter(
    String name,
    int maxLimit,
    ListSection cropSection,
    Consumer<Itinerary> changedSubscriber
  ) {
    this.name = name;
    this.maxLimit = maxLimit;
    this.cropSection = cropSection;
    this.changedSubscriber = changedSubscriber == null ? IGNORE_SUBSCRIBER : changedSubscriber;
  }

  @Override
  public String name() {
    return name;
  }

  @Override
  public List<Itinerary> flagForRemoval(List<Itinerary> itineraries) {
    if (itineraries.size() <= maxLimit) {
      return List.of();
    }

    if (cropSection == ListSection.HEAD) {
      int limit = itineraries.size() - maxLimit;
      changedSubscriber.accept(itineraries.get(limit - 1));
      return itineraries.subList(0, limit);
    }

    changedSubscriber.accept(itineraries.get(maxLimit));
    return itineraries.subList(maxLimit, itineraries.size());
  }
}
