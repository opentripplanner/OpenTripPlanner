package org.opentripplanner.routing.algorithm.filterchain.deletionflagger;

import java.util.List;
import java.util.OptionalDouble;
import java.util.stream.Collectors;
import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.routing.api.request.framework.DoubleAlgorithmFunction;
import org.opentripplanner.routing.api.request.preference.ItineraryFilterPreferences;

/**
 * This filter is similar to {@link TransitGeneralizedCostFilter}. There are some important
 * differences, however. It will only remove non-transit results, but ALL results can be used as a
 * basis for computing the cost limit.
 * <p>
 * This is needed so that we do not for example get walk legs that last several hours, when transit
 * can take you to the destination much quicker.
 * <p>
 *
 * @see ItineraryFilterPreferences#nonTransitGeneralizedCostLimit
 */
public class NonTransitGeneralizedCostFilter implements ItineraryDeletionFlagger {

  private final DoubleAlgorithmFunction costLimitFunction;

  public NonTransitGeneralizedCostFilter(DoubleAlgorithmFunction costLimitFunction) {
    this.costLimitFunction = costLimitFunction;
  }

  @Override
  public String name() {
    return "non-transit-cost-filter";
  }

  @Override
  public List<Itinerary> flagForRemoval(List<Itinerary> itineraries) {
    // ALL itineraries are considered here. Both transit and non-transit
    OptionalDouble minGeneralizedCost = itineraries
      .stream()
      .mapToDouble(Itinerary::getGeneralizedCost)
      .min();

    if (minGeneralizedCost.isEmpty()) {
      return List.of();
    }

    final double maxLimit = costLimitFunction.calculate(minGeneralizedCost.getAsDouble());

    return itineraries
      .stream()
      .filter(it -> !it.hasTransit() && it.getGeneralizedCost() > maxLimit)
      .collect(Collectors.toList());
  }
}
