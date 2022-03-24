package org.opentripplanner.routing.algorithm.filterchain.deletionflagger;

import java.util.stream.Collectors;
import org.opentripplanner.model.plan.Itinerary;

import java.util.List;
import java.util.OptionalDouble;
import java.util.function.DoubleFunction;

/**
 * This filter remove all transit results which have a generalized-cost higher than
 * the max-limit computed by the {@link #costLimitFunction}.
 * <p>
 * @see org.opentripplanner.routing.api.request.ItineraryFilterParameters#transitGeneralizedCostLimit
 */
public class TransitGeneralizedCostFilter implements ItineraryDeletionFlagger {
  private final DoubleFunction<Double> costLimitFunction;

  public TransitGeneralizedCostFilter(DoubleFunction<Double> costLimitFunction) {
    this.costLimitFunction = costLimitFunction;
  }

  @Override
  public String name() {
    return "transit-cost-filter";
  }

  @Override
  public List<Itinerary> getFlaggedItineraries(List<Itinerary> itineraries) {
    OptionalDouble minGeneralizedCost = itineraries
        .stream()
        .filter(Itinerary::hasTransit)
        .mapToDouble(it -> it.generalizedCost)
        .min();

    if(minGeneralizedCost.isEmpty()) { return List.of(); }

    final double maxLimit = costLimitFunction.apply(minGeneralizedCost.getAsDouble());

    return itineraries.stream()
      .filter( it -> it.hasTransit() && it.generalizedCost > maxLimit)
      .collect(Collectors.toList());
  }
}
