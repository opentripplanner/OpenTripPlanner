package org.opentripplanner.routing.algorithm.filterchain.filters.transit;

import org.opentripplanner.core.model.basic.Cost;
import org.opentripplanner.utils.tostring.ToStringBuilder;

/**
 * The RemoveTransitIfDirectIsBetter filter removes itineraries from a list of itineraries based on
 * a generalizedCostMaxLimit. Transit itineraries that have a higher generalized cost than the limit
 * will be filtered away. This results class is used as input for the PageCursor.
 */
public record RemoveTransitIfDirectIsBetterResult(Cost generalizedCostMaxLimit) {
  @Override
  public String toString() {
    return ToStringBuilder.of(RemoveTransitIfDirectIsBetterResult.class)
      .addObj("generalizedCostMaxLimit", generalizedCostMaxLimit)
      .toString();
  }
}
