package org.opentripplanner.routing.algorithm.filterchain.filters.transit;

import java.util.List;
import java.util.OptionalInt;
import javax.annotation.Nullable;
import org.opentripplanner.framework.model.Cost;
import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.routing.algorithm.filterchain.framework.spi.RemoveItineraryFlagger;
import org.opentripplanner.routing.api.request.framework.CostLinearFunction;

/**
 * Filter itineraries based on generalizedCost, compared with an on-street-all-the-way itinerary
 * (if it exists). If an itinerary cost exceeds the limit computed from the best
 * all-the-way-on-street itinerary, then the transit itinerary is removed.
 */
public class RemoveTransitIfStreetOnlyIsBetter implements RemoveItineraryFlagger {

  private final CostLinearFunction costLimitFunction;

  @Nullable
  private final Cost generalizedCostMaxLimit;

  private RemoveTransitIfStreetOnlyIsBetterResult removeTransitIfStreetOnlyIsBetterResult = null;

  /**
   * Constructs the RemoveTransitIfStreetOnlyIsBetter filter.
   * @param costLimitFunction the cost limit function to use with the filter
   * @param generalizedCostMaxLimit this limit is not null when paging is used
   */
  public RemoveTransitIfStreetOnlyIsBetter(
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
  public static final String TAG = "transit-vs-street-filter";

  @Override
  public String name() {
    return TAG;
  }

  @Override
  public List<Itinerary> flagForRemoval(List<Itinerary> itineraries) {
    Cost minStreetCost = null;

    if (generalizedCostMaxLimit != null) {
      // The best street only cost is used from the cursor, if it can be found.
      minStreetCost = generalizedCostMaxLimit;
    } else {
      // Find the best street-all-the-way option.
      OptionalInt minStreetCostOption = itineraries
        .stream()
        .filter(Itinerary::isStreetOnly)
        .mapToInt(Itinerary::generalizedCost)
        .min();

      if (minStreetCostOption.isPresent()) {
        minStreetCost = Cost.costOfSeconds(minStreetCostOption.getAsInt());
      }
    }

    // If no cost is found an empty list is returned.
    if (minStreetCost == null) {
      return List.of();
    }

    // This result is used for paging. It is collected by an aggregator.
    removeTransitIfStreetOnlyIsBetterResult = new RemoveTransitIfStreetOnlyIsBetterResult(
      minStreetCost
    );

    var limit = costLimitFunction.calculate(minStreetCost).toSeconds();

    // Filter away itineraries that have higher cost than limit cost computed above
    return itineraries
      .stream()
      // we use the cost without the access/egress penalty since we don't want to give
      // searches that are only on the street network an unfair advantage (they don't have
      // access/egress so cannot have these penalties)
      .filter(it -> !it.isStreetOnly() && it.generalizedCost() >= limit)
      .toList();
  }

  @Override
  public boolean skipAlreadyFlaggedItineraries() {
    return false;
  }

  public RemoveTransitIfStreetOnlyIsBetterResult getRemoveTransitIfStreetOnlyIsBetterResult() {
    return removeTransitIfStreetOnlyIsBetterResult;
  }
}
