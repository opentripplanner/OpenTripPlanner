package org.opentripplanner.routing.algorithm.filterchain.deletionflagger;

import java.util.Comparator;
import java.util.List;
import java.util.OptionalDouble;
import java.util.stream.Collectors;
import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.routing.api.request.framework.DoubleAlgorithmFunction;

/**
 * Filter itineraries based on generalizedCost, compared with a on-street-all-the-way itinerary(if
 * it exist). If an itinerary is slower than the best all-the-way-on-street itinerary, then the
 * transit itinerary is removed.
 */
public class RemoveTransitIfStreetOnlyIsBetterFilter implements ItineraryDeletionFlagger {

  private final DoubleAlgorithmFunction costLimitFunction;

  public RemoveTransitIfStreetOnlyIsBetterFilter(DoubleAlgorithmFunction costLimitFunction) {
    this.costLimitFunction = costLimitFunction;
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
    // Find the best walk-all-the-way option
    OptionalDouble minStreetCost = itineraries
      .stream()
      .filter(Itinerary::isOnStreetAllTheWay)
      .mapToDouble(Itinerary::getGeneralizedCost)
      .min();

    if (minStreetCost.isEmpty()) {
      return List.of();
    }

    final double limit = costLimitFunction.calculate(minStreetCost.getAsDouble());

    // Filter away itineraries that have higher cost than limit cost computed above
    return itineraries
      .stream()
      .filter(it -> !it.isOnStreetAllTheWay() && it.getGeneralizedCost() >= limit)
      .collect(Collectors.toList());
  }

  @Override
  public boolean skipAlreadyFlaggedItineraries() {
    return false;
  }
}
