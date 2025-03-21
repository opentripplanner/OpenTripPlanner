package org.opentripplanner.routing.algorithm.filterchain.filters.transit;

import java.util.List;
import java.util.OptionalInt;
import java.util.function.Consumer;
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

  private static final Consumer<RemoveTransitIfStreetOnlyIsBetterResults> IGNORE_SUBSCRIBER =
    i -> {};

  private final CostLinearFunction costLimitFunction;
  private final Cost generalizedCostMaxLimit;
  private final Consumer<
    RemoveTransitIfStreetOnlyIsBetterResults
  > removeTransitIfStreetOnlyIsBetterResultsSubscriber;

  /**
   * Constructs the RemoveTransitIfStreetOnlyIsBetter filter.
   * @param costLimitFunction the cost limit function to use with the filter
   * @param generalizedCostMaxLimit this limit is not null when paging is used
   * @param removeTransitIfStreetOnlyIsBetterResultsSubscriber this subscriber stores the generalizedCostMaxLimit for use with paging
   */
  public RemoveTransitIfStreetOnlyIsBetter(
    CostLinearFunction costLimitFunction,
    Cost generalizedCostMaxLimit,
    Consumer<
      RemoveTransitIfStreetOnlyIsBetterResults
    > removeTransitIfStreetOnlyIsBetterResultsSubscriber
  ) {
    this.costLimitFunction = costLimitFunction;
    this.generalizedCostMaxLimit = generalizedCostMaxLimit;
    this.removeTransitIfStreetOnlyIsBetterResultsSubscriber =
      removeTransitIfStreetOnlyIsBetterResultsSubscriber == null
        ? IGNORE_SUBSCRIBER
        : removeTransitIfStreetOnlyIsBetterResultsSubscriber;
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
    // Find the best street-all-the-way option
    OptionalInt minStreetCostOption = itineraries
      .stream()
      .filter(Itinerary::isStreetOnly)
      .mapToInt(Itinerary::generalizedCost)
      .min();
    Cost minStreetCost = null;

    if (minStreetCostOption.isEmpty() && generalizedCostMaxLimit == null) {
      // If no cost is found an empty list is returned.
      return List.of();
    } else if (minStreetCostOption.isPresent() && generalizedCostMaxLimit != null) {
      // This case should not be possible.
      throw new UnsupportedOperationException(
        "Both the minStreetCostOption and generalizedCostMaxLimit are present, this should never happen."
      );
    } else if (generalizedCostMaxLimit != null) {
      // If the best street only cost can't be found in the itineraries but
      // it is present in the cursor, then the information from the cursor is used.
      minStreetCost = generalizedCostMaxLimit;
    } else {
      // The minStreetCostOption is present.
      minStreetCost = Cost.costOfSeconds(minStreetCostOption.getAsInt());
    }

    // The best street only cost is saved in the cursor.
    removeTransitIfStreetOnlyIsBetterResultsSubscriber.accept(
      new RemoveTransitIfStreetOnlyIsBetterResults(minStreetCost)
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
}
