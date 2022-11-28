package org.opentripplanner.routing.algorithm.filterchain.api;

import org.opentripplanner.framework.lang.DoubleUtils;
import org.opentripplanner.routing.algorithm.filterchain.deletionflagger.TransitGeneralizedCostFilter;
import org.opentripplanner.routing.api.request.framework.DoubleAlgorithmFunction;

/**
 * Input parameters for {@link TransitGeneralizedCostFilter}
 *
 * @param costLimitFunction   Describes the function to calculate the limit for an itinerary based
 *                            on the generalized cost
 * @param intervalRelaxFactor Describes the multiplier of how much to increase the limit either from
 *                            the difference between the departure or arrival times between the two
 *                            itineraries, whichever is the greatest
 */
public record TransitGeneralizedCostFilterParams(
  DoubleAlgorithmFunction costLimitFunction,
  double intervalRelaxFactor
) {
  public TransitGeneralizedCostFilterParams(
    DoubleAlgorithmFunction costLimitFunction,
    double intervalRelaxFactor
  ) {
    if (intervalRelaxFactor < 0.0) {
      throw new IllegalArgumentException("Negative value not expected: " + intervalRelaxFactor);
    }
    this.costLimitFunction = costLimitFunction;
    this.intervalRelaxFactor = DoubleUtils.roundTo2Decimals(intervalRelaxFactor);
  }
}
