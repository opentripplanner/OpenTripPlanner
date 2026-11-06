package org.opentripplanner.routing.algorithm.filterchain.filters.transit;

import java.time.temporal.ChronoUnit;
import org.opentripplanner.core.model.basic.Cost;
import org.opentripplanner.core.model.basic.CostLinearFunction;
import org.opentripplanner.model.plan.Itinerary;

/**
 * A relative limit for the generalized-cost of an itinerary, calculated from a reference itinerary:
 * <pre>
 * limit = costLimitFunction(referenceCost) + intervalRelaxFactor * min(|Δdeparture|, |Δarrival|)
 * </pre>
 * The first term is calculated from the generalized-cost of the reference. The second term adds
 * slack for how far apart in time the itinerary and the reference are. The time differences are in
 * seconds.
 */
class GeneralizedCostLimit {

  private final CostLinearFunction costLimitFunction;

  private final double intervalRelaxFactor;

  GeneralizedCostLimit(CostLinearFunction costLimitFunction, double intervalRelaxFactor) {
    this.costLimitFunction = costLimitFunction;
    this.intervalRelaxFactor = intervalRelaxFactor;
  }

  /**
   * Return true if the generalized-cost of the subject is higher than the limit calculated from
   * the reference.
   */
  boolean exceedsLimit(Itinerary subject, Itinerary reference) {
    return subject
      .generalizedCostIncludingPenalty()
      .greaterThan(calculateLimit(subject, reference));
  }

  private Cost calculateLimit(Itinerary subject, Itinerary reference) {
    return costLimitFunction
      .calculate(reference.generalizedCostIncludingPenalty())
      .plus(getWaitTimeCost(reference, subject));
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
