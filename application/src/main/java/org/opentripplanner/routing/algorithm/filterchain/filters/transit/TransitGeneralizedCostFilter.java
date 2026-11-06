package org.opentripplanner.routing.algorithm.filterchain.filters.transit;

import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import org.opentripplanner.core.model.basic.Cost;
import org.opentripplanner.core.model.basic.CostLinearFunction;
import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.routing.algorithm.filterchain.framework.spi.RemoveItineraryFlagger;

/**
 * This filter removes all transit results which have a generalized-cost higher than the max-limit
 * computed by the {@link #costLimitFunction} plus the wait cost given by
 * {@link TransitGeneralizedCostFilter#getWaitTimeCost}.
 * <p>
 * Does not apply to direct flex itineraries. They are not used in the comparison, and they can
 * not get flagged for removal by this filter.
 */
public class TransitGeneralizedCostFilter implements RemoveItineraryFlagger {

  private final CostLinearFunction costLimitFunction;

  private final double intervalRelaxFactor;

  public TransitGeneralizedCostFilter(CostLinearFunction costLimit, double intervalRelaxFactor) {
    this.costLimitFunction = costLimit;
    this.intervalRelaxFactor = intervalRelaxFactor;
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
        transitItinerariesExceptDirectFlex
          .stream()
          .anyMatch(t -> generalizedCostExceedsLimit(it, t))
      )
      .collect(Collectors.toList());
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
