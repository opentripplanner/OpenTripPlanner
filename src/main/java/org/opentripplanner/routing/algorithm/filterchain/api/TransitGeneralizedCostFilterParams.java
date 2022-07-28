package org.opentripplanner.routing.algorithm.filterchain.api;

import java.util.function.DoubleFunction;
import org.opentripplanner.routing.algorithm.filterchain.deletionflagger.TransitGeneralizedCostFilter;

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
  DoubleFunction<Double> costLimitFunction,
  double intervalRelaxFactor
) {}
