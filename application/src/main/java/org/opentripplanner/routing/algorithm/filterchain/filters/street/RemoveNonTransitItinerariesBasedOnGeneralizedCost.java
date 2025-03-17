package org.opentripplanner.routing.algorithm.filterchain.filters.street;

import java.util.List;
import java.util.OptionalInt;
import java.util.stream.Collectors;
import org.opentripplanner.framework.model.Cost;
import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.routing.algorithm.filterchain.filters.transit.TransitGeneralizedCostFilter;
import org.opentripplanner.routing.algorithm.filterchain.framework.spi.RemoveItineraryFlagger;
import org.opentripplanner.routing.api.request.framework.CostLinearFunction;
import org.opentripplanner.routing.api.request.preference.ItineraryFilterPreferences;

/**
 * This filter remove none-transit itineraries with generalized-cost higher than the max-limit.
 * The max-limit is computed based on the overall min-generalized-cost using the provided cost
 * function.
 * <p>
 * This filter is similar to {@link TransitGeneralizedCostFilter}. There are some important
 * differences, however. It will only remove non-transit results, but ALL results can be used as a
 * basis for computing the cost limit.
 * <p>
 * This will, for example, remove walk legs which last several hours, when transit can take you to
 * the destination much quicker.
 * <p>
 *
 * @see ItineraryFilterPreferences#nonTransitGeneralizedCostLimit()
 */
public class RemoveNonTransitItinerariesBasedOnGeneralizedCost implements RemoveItineraryFlagger {

  private final CostLinearFunction costLimitFunction;

  public RemoveNonTransitItinerariesBasedOnGeneralizedCost(CostLinearFunction costLimitFunction) {
    this.costLimitFunction = costLimitFunction;
  }

  @Override
  public String name() {
    return "non-transit-cost-filter";
  }

  @Override
  public List<Itinerary> flagForRemoval(List<Itinerary> itineraries) {
    // ALL itineraries are considered here. Both transit and non-transit
    OptionalInt minGeneralizedCost = itineraries
      .stream()
      .mapToInt(Itinerary::generalizedCost)
      .min();

    if (minGeneralizedCost.isEmpty()) {
      return List.of();
    }

    // TODO: This is a bit ugly, but the filters should be refactored
    //       to use the Cost type and not int.
    var maxLimit = costLimitFunction
      .calculate(Cost.costOfSeconds(minGeneralizedCost.getAsInt()))
      .toSeconds();

    return itineraries
      .stream()
      .filter(it -> !it.hasTransit() && it.generalizedCost() > maxLimit)
      .collect(Collectors.toList());
  }
}
