package org.opentripplanner.routing.algorithm.filterchain.filter;

import static org.opentripplanner.routing.algorithm.filterchain.comparator.SortOrderComparator.numberOfTransfersComparator;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.opentripplanner.model.SystemNotice;
import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.routing.algorithm.filterchain.ItineraryListFilter;

/**
 * This filter makes sure that the itinerary with the least amount of transfers is not marked for
 * deletion. It iterates over the itineraries and removes the SystemNotice if it contains
 * the provided set of {@code filterKeys}. The itinerary must match all {@code filterKeys}, and
 * if so the given keys are removed. Other system-notices are ignored.
 */
public class RemoveDeletionFlagForLeastTransfersItinerary implements ItineraryListFilter {

  private final Set<String> filterKeys;

  public RemoveDeletionFlagForLeastTransfersItinerary(List<String> filterKeys) {
    this.filterKeys = new HashSet<>(filterKeys);
  }

  @Override
  public List<Itinerary> filter(List<Itinerary> itineraries) {
    itineraries
      .stream()
      .min(numberOfTransfersComparator())
      .filter(it ->
        filterKeys.containsAll(it.getSystemNotices().stream().map(SystemNotice::tag).toList())
      )
      .ifPresent(it -> it.removeDeletionFlags(filterKeys));

    return itineraries;
  }
}
