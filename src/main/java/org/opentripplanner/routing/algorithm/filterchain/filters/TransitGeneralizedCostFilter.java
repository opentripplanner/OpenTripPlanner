package org.opentripplanner.routing.algorithm.filterchain.filters;

import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.routing.algorithm.filterchain.ItineraryFilter;

import java.util.List;
import java.util.OptionalDouble;
import java.util.function.DoubleFunction;
import java.util.stream.Collectors;

/**
 * This filter remove all transit results witch have a generalized-cost higher than
 * the max-limit computed by the {@link #costLimitFunction}.
 * <p>
 * @see org.opentripplanner.routing.api.request.ItineraryFilterParameters#transitGeneralizedCostLimit
 */
public class TransitGeneralizedCostFilter implements ItineraryFilter {
  private final DoubleFunction<Double> costLimitFunction;

  public TransitGeneralizedCostFilter(DoubleFunction<Double> costLimitFunction) {
    this.costLimitFunction = costLimitFunction;
  }

  @Override
  public String name() {
    return "transit-cost-filter";
  }

  @Override
  public List<Itinerary> filter(List<Itinerary> itineraries) {
    OptionalDouble minGeneralizedCost = itineraries
        .stream()
        .filter(Itinerary::hasTransit)
        .mapToDouble(it -> it.generalizedCost)
        .min();

    if(minGeneralizedCost.isEmpty()) { return itineraries; }

    final double maxLimit = costLimitFunction.apply(minGeneralizedCost.getAsDouble());

    return itineraries.stream().filter(
      it -> !it.hasTransit() || it.generalizedCost <= maxLimit
    ).collect(Collectors.toList());
  }

  @Override
  public boolean removeItineraries() {
    return true;
  }
}
