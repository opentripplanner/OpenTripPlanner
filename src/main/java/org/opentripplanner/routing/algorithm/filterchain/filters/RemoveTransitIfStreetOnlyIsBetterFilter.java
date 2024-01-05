package org.opentripplanner.routing.algorithm.filterchain.filters;

import java.util.List;
import java.util.OptionalInt;
import java.util.stream.Collectors;
import org.opentripplanner.framework.model.Cost;
import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.routing.algorithm.filterchain.framework.spi.ItineraryDeletionFlagger;
import org.opentripplanner.routing.api.request.framework.CostLinearFunction;

/**
 * Filter itineraries based on generalizedCost, compared with a on-street-all-the-way itinerary(if
 * it exist). If an itinerary cost exceeds the limit computed from the best all-the-way-on-street itinerary, then the
 * transit itinerary is removed.
 */
public class RemoveTransitIfStreetOnlyIsBetterFilter implements ItineraryDeletionFlagger {

  private final CostLinearFunction costLimitFunction;

  public RemoveTransitIfStreetOnlyIsBetterFilter(CostLinearFunction costLimitFunction) {
    this.costLimitFunction = costLimitFunction;
  }

  /**
   * Required for {@link org.opentripplanner.routing.algorithm.filterchain.ItineraryListFilterChain},
   * to know which filters removed
   */
  public static final String TAG = "transit-vs-street-filter";

  @Override
  public String name() {
    return TAG;
  }

  @Override
  public List<Itinerary> flagForRemoval(List<Itinerary> itineraries) {
    // Find the best street-all-the-way option
    OptionalInt minStreetCost = itineraries
      .stream()
      .filter(Itinerary::isOnStreetAllTheWay)
      .mapToInt(Itinerary::getGeneralizedCost)
      .min();

    if (minStreetCost.isEmpty()) {
      return List.of();
    }

    var limit = costLimitFunction
      .calculate(Cost.costOfSeconds(minStreetCost.getAsInt()))
      .toSeconds();

    // Filter away itineraries that have higher cost than limit cost computed above
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
