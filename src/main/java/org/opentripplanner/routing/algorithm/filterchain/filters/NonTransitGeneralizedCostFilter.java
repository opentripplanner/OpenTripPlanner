package org.opentripplanner.routing.algorithm.filterchain.filters;

import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.routing.algorithm.filterchain.ItineraryFilter;

import java.util.List;
import java.util.OptionalDouble;
import java.util.function.DoubleFunction;
import java.util.stream.Collectors;

/**
 * This filter is similar to {@link TransitGeneralizedCostFilter}, but it will only remove
 * non-transit results.
 * <p>
 * @see org.opentripplanner.routing.api.request.ItineraryFilterParameters#nonTransitGeneralizedCostLimit
 */
public class NonTransitGeneralizedCostFilter implements ItineraryFilter {
  private final DoubleFunction<Double> costLimitFunction;

  public NonTransitGeneralizedCostFilter(DoubleFunction<Double> costLimitFunction) {
    this.costLimitFunction = costLimitFunction;
  }

  @Override
  public String name() {
    return "non-transit-cost-filter";
  }

  @Override
  public List<Itinerary> filter(List<Itinerary> itineraries) {
    OptionalDouble minGeneralizedCost = itineraries
        .stream()
        .mapToDouble(it -> it.generalizedCost)
        .min();

    if(minGeneralizedCost.isEmpty()) { return itineraries; }

    final double maxLimit = costLimitFunction.apply(minGeneralizedCost.getAsDouble());

    return itineraries.stream().filter(
      it -> it.hasTransit() || it.generalizedCost <= maxLimit
    ).collect(Collectors.toList());
  }

  @Override
  public boolean removeItineraries() {
    return true;
  }
}
