package org.opentripplanner.routing.algorithm.filterchain.framework.filter;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.model.plan.ItinerarySortKey;
import org.opentripplanner.routing.algorithm.filterchain.framework.spi.ItineraryListFilter;

/**
 * This is a filter to sort itineraries. To create a filter, provide a comparator as a constructor
 * argument.
 */
public final class SortingFilter implements ItineraryListFilter {

  private final Comparator<ItinerarySortKey> comparator;

  public SortingFilter(Comparator<ItinerarySortKey> comparator) {
    this.comparator = comparator;
  }

  public Comparator<ItinerarySortKey> comparator() {
    return comparator;
  }

  @Override
  public List<Itinerary> filter(List<Itinerary> itineraries) {
    if (itineraries.size() < 2) {
      return itineraries;
    }
    // Sort ascending by qualifier and map to list of itineraries
    return itineraries.stream().sorted(comparator()).collect(Collectors.toList());
  }
}
