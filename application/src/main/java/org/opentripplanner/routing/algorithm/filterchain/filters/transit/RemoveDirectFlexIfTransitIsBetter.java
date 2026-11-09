package org.opentripplanner.routing.algorithm.filterchain.filters.transit;

import java.util.List;
import org.opentripplanner.core.model.basic.CostLinearFunction;
import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.routing.algorithm.filterchain.framework.spi.RemoveItineraryFlagger;

/**
 * This filter removes direct-flex itineraries which have a generalized-cost higher than the
 * {@link GeneralizedCostLimit} computed from any of the other transit itineraries, including other
 * direct-flex itineraries. The limit is the same as in {@link TransitGeneralizedCostFilter}.
 * <p>
 * Direct flex has its own filter, so that it can be weighted differently from other transit.
 */
public class RemoveDirectFlexIfTransitIsBetter implements RemoveItineraryFlagger {

  private final GeneralizedCostLimit costLimit;

  public RemoveDirectFlexIfTransitIsBetter(
    CostLinearFunction costLimit,
    double intervalRelaxFactor
  ) {
    this.costLimit = new GeneralizedCostLimit(costLimit, intervalRelaxFactor);
  }

  @Override
  public String name() {
    return "direct-flex-cost-filter";
  }

  @Override
  public List<Itinerary> flagForRemoval(List<Itinerary> itineraries) {
    List<Itinerary> transitItineraries = itineraries
      .stream()
      .filter(Itinerary::hasTransit)
      .toList();

    return itineraries
      .stream()
      .filter(Itinerary::hasTransit)
      .filter(Itinerary::isDirectFlex)
      .filter(it -> transitItineraries.stream().anyMatch(t -> costLimit.exceedsLimit(it, t)))
      .toList();
  }
}
