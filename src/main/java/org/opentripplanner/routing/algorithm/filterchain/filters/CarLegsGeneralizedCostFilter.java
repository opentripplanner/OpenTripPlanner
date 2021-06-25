package org.opentripplanner.routing.algorithm.filterchain.filters;

import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.routing.algorithm.filterchain.ItineraryFilter;
import org.opentripplanner.routing.core.TraverseMode;

import java.util.List;
import java.util.OptionalDouble;
import java.util.function.DoubleFunction;
import java.util.stream.Collectors;

/**
 * This filter is made to filter out itineraries with long car legs when there exist other options
 * with much shorter car legs. This is to discourage car use in favour of transit.
 *
 * The car mode is a special case compared to other non-transit modes, since it is often quite a
 * lot faster than transit.
 *
 * This filter is needed in addition to a high carReluctance, because no matter how high we set
 * that cost we will get some results that have a very high cost but are optimal because they are
 * faster.
 *
 * @see org.opentripplanner.routing.api.request.ItineraryFilterParameters#carLegsGeneralizedCostLimit
 */
public class CarLegsGeneralizedCostFilter implements ItineraryFilter {

  private final DoubleFunction<Double> costLimitFunction;

  public CarLegsGeneralizedCostFilter(DoubleFunction<Double> costLimitFunction) {
    this.costLimitFunction = costLimitFunction;
  }

  @Override
  public String name() {
    return "non-transit-cost-filter";
  }

  @Override
  public List<Itinerary> filter(List<Itinerary> itineraries) {
    // Only itineraries that contain car legs are considered here
    OptionalDouble minGeneralizedCostForCarLegs = itineraries
        .stream()
        .filter(this::containsCarLegs)
        .mapToDouble(this::getGeneralizedCostForCarLegs)
        .min();

    if (minGeneralizedCostForCarLegs.isEmpty()) { return itineraries; }

    final double maxLimit = costLimitFunction.apply(minGeneralizedCostForCarLegs.getAsDouble());

    // Only itineraries that contain car legs are filtered out
    return itineraries
        .stream()
        .filter(it -> !containsCarLegs(it) || getGeneralizedCostForCarLegs(it) <= maxLimit)
        .collect(Collectors.toList());
  }

  @Override
  public boolean removeItineraries() {
    return true;
  }

  private boolean containsCarLegs(Itinerary itinerary) {
    return itinerary.legs.stream().anyMatch(l -> l.mode == TraverseMode.CAR);
  }

  /**
   * Returns the generalized cost for the car legs of the itinerary
   */
  private double getGeneralizedCostForCarLegs(Itinerary itinerary) {
    return itinerary.legs
        .stream()
        .filter(l -> l.mode == TraverseMode.CAR)
        .mapToDouble(l -> l.generalizedCost)
        .sum();
  }
}
