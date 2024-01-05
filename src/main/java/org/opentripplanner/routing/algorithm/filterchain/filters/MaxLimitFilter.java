package org.opentripplanner.routing.algorithm.filterchain.filters;

import java.util.List;
import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.routing.algorithm.filterchain.framework.spi.RemoveItineraryFlagger;

/**
 * Flags the itineraries at the end of the list for removal. The list should be sorted on the
 * desired key before this filter is applied.
 */
public class MaxLimitFilter implements RemoveItineraryFlagger {

  private final String name;
  private final int maxLimit;

  public MaxLimitFilter(String name, int maxLimit) {
    this.name = name;
    this.maxLimit = maxLimit;
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

    return itineraries.subList(maxLimit, itineraries.size());
  }
}
