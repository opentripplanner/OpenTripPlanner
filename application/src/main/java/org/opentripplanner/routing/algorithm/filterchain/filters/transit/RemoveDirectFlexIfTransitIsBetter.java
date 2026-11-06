package org.opentripplanner.routing.algorithm.filterchain.filters.transit;

import java.time.temporal.ChronoUnit;
import java.util.List;
import org.opentripplanner.core.model.basic.Cost;
import org.opentripplanner.core.model.basic.CostLinearFunction;
import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.routing.algorithm.filterchain.framework.spi.RemoveItineraryFlagger;

/**
 * This filter removes direct-flex itineraries which have a generalized-cost higher than the
 * max-limit computed by the {@link #costLimitFunction} plus the wait cost given by
 * {@link #getWaitTimeCost}. The limit is computed from each of the other transit itineraries,
 * in the same way as in {@link TransitGeneralizedCostFilter}.
 * <p>
 * Direct flex has its own filter, so that it can be weighted differently from other transit.
 */
public class RemoveDirectFlexIfTransitIsBetter implements RemoveItineraryFlagger {

  private final CostLinearFunction costLimitFunction;

  private final double intervalRelaxFactor;

  public RemoveDirectFlexIfTransitIsBetter(
    CostLinearFunction costLimit,
    double intervalRelaxFactor
  ) {
    this.costLimitFunction = costLimit;
    this.intervalRelaxFactor = intervalRelaxFactor;
  }

  @Override
  public String name() {
    return "direct-flex-cost-filter";
  }

  @Override
  public List<Itinerary> flagForRemoval(List<Itinerary> itineraries) {
    List<Itinerary> transitItinerariesExceptDirectFlex = itineraries
      .stream()
      .filter(Itinerary::hasTransit)
      .filter(it -> !it.isDirectFlex())
      .toList();

    return itineraries
      .stream()
      .filter(Itinerary::hasTransit)
      .filter(Itinerary::isDirectFlex)
      .filter(it ->
        transitItinerariesExceptDirectFlex
          .stream()
          .anyMatch(t -> generalizedCostExceedsLimit(it, t))
      )
      .toList();
  }

  private boolean generalizedCostExceedsLimit(Itinerary subject, Itinerary transitItinerary) {
    return subject
      .generalizedCostIncludingPenalty()
      .greaterThan(calculateLimit(subject, transitItinerary));
  }

  private Cost calculateLimit(Itinerary subject, Itinerary transitItinerary) {
    return costLimitFunction
      .calculate(transitItinerary.generalizedCostIncludingPenalty())
      .plus(getWaitTimeCost(transitItinerary, subject));
  }

  private Cost getWaitTimeCost(Itinerary a, Itinerary b) {
    return Cost.costOfSeconds(
      intervalRelaxFactor *
        Math.min(
          Math.abs(ChronoUnit.SECONDS.between(a.startTime(), b.startTime())),
          Math.abs(ChronoUnit.SECONDS.between(a.endTime(), b.endTime()))
        )
    );
  }
}
