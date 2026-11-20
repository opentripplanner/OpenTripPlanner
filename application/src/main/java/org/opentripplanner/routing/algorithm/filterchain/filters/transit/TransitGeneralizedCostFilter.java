package org.opentripplanner.routing.algorithm.filterchain.filters.transit;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import org.opentripplanner.core.model.basic.CostLinearFunction;
import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.routing.algorithm.filterchain.framework.spi.RemoveItineraryFlagger;

/**
 * This filter removes all transit results which have a generalized-cost higher than the
 * {@link GeneralizedCostLimit} computed from any of the other transit results.
 * <p>
 * Does not apply to direct flex itineraries. They are not used in the comparison, and they can
 * not get flagged for removal by this filter.
 */
public class TransitGeneralizedCostFilter implements RemoveItineraryFlagger {

  private final GeneralizedCostLimit costLimit;

  public TransitGeneralizedCostFilter(CostLinearFunction costLimit, double intervalRelaxFactor) {
    this.costLimit = new GeneralizedCostLimit(costLimit, intervalRelaxFactor);
  }

  @Override
  public String name() {
    return "transit-cost-filter";
  }

  @Override
  public List<Itinerary> flagForRemoval(List<Itinerary> itineraries) {
    List<Itinerary> transitItinerariesExceptDirectFlex = itineraries
      .stream()
      .filter(Itinerary::hasTransit)
      .filter(it -> !it.isDirectFlex())
      .sorted(Comparator.comparing(Itinerary::generalizedCostIncludingPenalty))
      .toList();

    return transitItinerariesExceptDirectFlex
      .stream()
      .filter(it ->
        transitItinerariesExceptDirectFlex.stream().anyMatch(t -> costLimit.exceedsLimit(it, t))
      )
      .collect(Collectors.toList());
  }
}
