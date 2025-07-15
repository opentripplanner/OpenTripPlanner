package org.opentripplanner.routing.algorithm.filterchain.filters.transit;

import java.util.List;
import java.util.OptionalInt;
import java.util.stream.Collectors;
import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.routing.algorithm.filterchain.framework.spi.RemoveItineraryFlagger;

/**
 * Filter itineraries which have a higher generalized-cost than the walk-only itinerary (if exist).
 */
public class RemoveTransitIfWalkingIsBetter implements RemoveItineraryFlagger {

  /**
   * Required for {@link org.opentripplanner.routing.algorithm.filterchain.ItineraryListFilterChain},
   * to know which filters removed
   */
  public static final String TAG = "transit-vs-walk-filter";

  @Override
  public String name() {
    return TAG;
  }

  @Override
  public List<Itinerary> flagForRemoval(List<Itinerary> itineraries) {
    OptionalInt minWalkCost = itineraries
      .stream()
      .filter(itinerary -> itinerary.isWalkOnly())
      .mapToInt(Itinerary::generalizedCost)
      .min();

    if (minWalkCost.isEmpty()) {
      return List.of();
    }

    var limit = minWalkCost.getAsInt();

    return itineraries
      .stream()
      // we use the cost without the access/egress penalty since we don't want to give
      // searches that are only on the street network an unfair advantage
      .filter(it -> !it.isStreetOnly() && it.generalizedCost() >= limit)
      .collect(Collectors.toList());
  }

  @Override
  public boolean skipAlreadyFlaggedItineraries() {
    return false;
  }
}
