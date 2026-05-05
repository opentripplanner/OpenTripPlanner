package org.opentripplanner.routing.algorithm.filterchain.filters.transit;

import java.util.List;
import java.util.OptionalInt;
import javax.annotation.Nullable;
import org.opentripplanner.core.model.basic.Cost;
import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.routing.algorithm.filterchain.framework.spi.RemoveItineraryFlagger;
import org.opentripplanner.routing.api.request.framework.CostLinearFunction;

/**
 * Filter itineraries based on generalizedCost, compared with the best direct itinerary
 * (street-only or direct-flex, if it exists). If an itinerary cost exceeds the limit computed
 * from the best direct itinerary, then the transit itinerary is removed.
 */
public class RemoveTransitIfDirectIsBetter implements RemoveItineraryFlagger {

  private final CostLinearFunction costLimitFunction;

  @Nullable
  private final Cost generalizedCostMaxLimit;

  private RemoveTransitIfDirectIsBetterResult removeTransitIfDirectIsBetterResult = null;

  /**
   * Constructs the RemoveTransitIfDirectIsBetter filter.
   * @param costLimitFunction the cost limit function to use with the filter
   * @param generalizedCostMaxLimit this limit is not null when paging is used
   */
  public RemoveTransitIfDirectIsBetter(
    CostLinearFunction costLimitFunction,
    @Nullable Cost generalizedCostMaxLimit
  ) {
    this.costLimitFunction = costLimitFunction;
    this.generalizedCostMaxLimit = generalizedCostMaxLimit;
  }

  /**
   * Required for {@link org.opentripplanner.routing.algorithm.filterchain.ItineraryListFilterChain},
   * to know which filters removed
   */
  public static final String TAG = "transit-vs-direct-filter";

  @Override
  public String name() {
    return TAG;
  }

  @Override
  public List<Itinerary> flagForRemoval(List<Itinerary> itineraries) {
    Cost minDirectCost = null;

    if (generalizedCostMaxLimit != null) {
      // The best direct cost is used from the cursor, if it can be found.
      minDirectCost = generalizedCostMaxLimit;
    } else {
      // Find the best direct option (street-only or direct-flex).
      OptionalInt minDirectCostOption = itineraries
        .stream()
        .filter(it -> it.isStreetOnly() || it.isDirectFlex())
        .mapToInt(Itinerary::generalizedCost)
        .min();

      if (minDirectCostOption.isPresent()) {
        minDirectCost = Cost.costOfSeconds(minDirectCostOption.getAsInt());
      }
    }

    // If no cost is found an empty list is returned.
    if (minDirectCost == null) {
      return List.of();
    }

    // This result is used for paging. It is collected by an aggregator.
    removeTransitIfDirectIsBetterResult = new RemoveTransitIfDirectIsBetterResult(minDirectCost);

    var limit = costLimitFunction.calculate(minDirectCost).toSeconds();

    // Filter away itineraries that have higher cost than limit cost computed above
    return itineraries
      .stream()
      // we use the cost without the access/egress penalty since we don't want to give
      // direct itineraries an unfair advantage (they don't have access/egress so cannot
      // have these penalties)
      .filter(it -> !it.isStreetOnly() && !it.isDirectFlex() && it.generalizedCost() >= limit)
      .toList();
  }

  @Override
  public boolean skipAlreadyFlaggedItineraries() {
    return false;
  }

  public RemoveTransitIfDirectIsBetterResult getRemoveTransitIfDirectIsBetterResult() {
    return removeTransitIfDirectIsBetterResult;
  }
}
