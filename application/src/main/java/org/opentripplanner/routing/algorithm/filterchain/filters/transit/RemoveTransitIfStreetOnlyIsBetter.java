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

  private static final Consumer<OptionalInt> IGNORE_SUBSCRIBER = i -> {};

  private final CostLinearFunction costLimitFunction;
  private final OptionalInt bestStreetOnlyCost;
  private final Consumer<OptionalInt> bestStreetOnlyCostSubscriber;

  public RemoveTransitIfStreetOnlyIsBetter(
    CostLinearFunction costLimitFunction,
    OptionalInt bestStreetOnlyCost,
    Consumer<OptionalInt> bestStreetOnlyCostSubscriber
  ) {
    this.costLimitFunction = costLimitFunction;
    this.bestStreetOnlyCost = bestStreetOnlyCost;
    this.bestStreetOnlyCostSubscriber =
      bestStreetOnlyCostSubscriber == null ? IGNORE_SUBSCRIBER : bestStreetOnlyCostSubscriber;
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
    OptionalInt minStreetCost = itineraries
      .stream()
      .filter(Itinerary::isOnStreetAllTheWay)
      .mapToInt(Itinerary::getGeneralizedCost)
      .min();

    // If the best street only cost can't be found in the itineraries but
    // it is present in the cursor, then the information from the cursor is used.
    // If no cost is found an empty list is returned.
    if (minStreetCost.isEmpty() && bestStreetOnlyCost.isPresent()) {
      minStreetCost = bestStreetOnlyCost;
    } else {
      return List.of();
    }

    // The best street only cost is saved in the cursor.
    bestStreetOnlyCostSubscriber.accept(minStreetCost);

    var limit = costLimitFunction
      .calculate(Cost.costOfSeconds(minStreetCost.getAsInt()))
      .toSeconds();

    // Filter away itineraries that have higher cost than limit cost computed above
    return itineraries
      .stream()
      // we use the cost without the access/egress penalty since we don't want to give
      // searches that are only on the street network an unfair advantage (they don't have
      // access/egress so cannot have these penalties)
      .filter(it -> !it.isOnStreetAllTheWay() && it.getGeneralizedCost() >= limit)
      .toList();
  }

  @Override
  public boolean skipAlreadyFlaggedItineraries() {
    return false;
  }
}
