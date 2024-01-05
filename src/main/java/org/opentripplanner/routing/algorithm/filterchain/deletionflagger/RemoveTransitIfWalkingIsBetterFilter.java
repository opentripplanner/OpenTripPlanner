package org.opentripplanner.routing.algorithm.filterchain.deletionflagger;

import java.util.List;
import java.util.OptionalInt;
import java.util.stream.Collectors;
import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.routing.algorithm.filterchain.framework.spi.ItineraryDeletionFlagger;

/**
 * Filter itineraries which have a higher generalized-cost than a pure walk itinerary.
 */
public class RemoveTransitIfWalkingIsBetterFilter implements ItineraryDeletionFlagger {

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
      .filter(Itinerary::isWalkingAllTheWay)
      .mapToInt(Itinerary::getGeneralizedCost)
      .min();

    if (minWalkCost.isEmpty()) {
      return List.of();
    }

    var limit = minWalkCost.getAsInt();

    return itineraries
      .stream()
      .filter(it -> !it.isOnStreetAllTheWay() && it.getGeneralizedCost() >= limit)
      .collect(Collectors.toList());
  }

  @Override
  public boolean skipAlreadyFlaggedItineraries() {
    return false;
  }
}
