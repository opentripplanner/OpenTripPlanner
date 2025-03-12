package org.opentripplanner.routing.algorithm.filterchain.filters.transit;

import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import org.opentripplanner.framework.model.Cost;
import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.routing.algorithm.filterchain.framework.spi.RemoveItineraryFlagger;
import org.opentripplanner.routing.api.request.framework.CostLinearFunction;

/**
 * This filter removes all transit results which have a generalized-cost higher than the max-limit
 * computed by the {@link #costLimitFunction} plus the wait cost given by
 * {@link TransitGeneralizedCostFilter#getWaitTimeCost}.
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
    List<Itinerary> transitItineraries = itineraries
      .stream()
      .filter(Itinerary::hasTransit)
      .sorted(Comparator.comparing(Itinerary::generalizedCostIncludingPenalty))
      .toList();

    return transitItineraries
      .stream()
      .filter(it -> transitItineraries.stream().anyMatch(t -> generalizedCostExceedsLimit(it, t)))
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
