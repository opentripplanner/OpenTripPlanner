package org.opentripplanner.routing.algorithm.filterchain.deletionflagger;

import java.util.Comparator;
import java.util.List;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.stream.Collectors;
import org.opentripplanner.framework.model.Cost;
import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.model.plan.Leg;
import org.opentripplanner.routing.api.request.framework.CostLinearFunction;

/**
 * Filter itineraries based on generalizedCost, compared with a on-street-all-the-way itinerary(if
 * it exist). If an itinerary is slower than the best all-the-way-on-street itinerary, then the
 * transit itinerary is removed.
 */
public class RemoveTransitIfStreetOnlyIsBetterFilter implements ItineraryDeletionFlagger {

  private final CostLinearFunction costLimitFunction;

  public RemoveTransitIfStreetOnlyIsBetterFilter(CostLinearFunction costLimitFunction) {
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

  private double getWalkDistance(Itinerary it) {
    return it
      .getStreetLegs()
      .filter(l -> l.isWalkingLeg())
      .mapToDouble(Leg::getDistanceMeters)
      .sum();
  }

  @Override
  public List<Itinerary> flagForRemoval(List<Itinerary> itineraries) {
    // Find the best walk-all-the-way option
    OptionalInt minStreetCost = itineraries
      .stream()
      .filter(Itinerary::isOnStreetAllTheWay)
      .mapToInt(Itinerary::getGeneralizedCost)
      .min();

    if (minStreetCost.isEmpty()) {
      return List.of();
    }

    var limit = costLimitFunction
      .calculate(Cost.costOfSeconds(minStreetCost.getAsInt()))
      .toSeconds();

    // Filter away itineraries that have higher cost than limit cost computed above
    List<Itinerary> filtered = itineraries
      .stream()
      .filter(it -> !it.isOnStreetAllTheWay() && it.getGeneralizedCost() >= limit)
      .collect(Collectors.toList());

    // Filter the most common silly itinerary case: transit itinerary has more walking than plain walk itinerary
    // This never makes sense
    OptionalDouble walkDistance = itineraries
      .stream()
      .filter(Itinerary::isWalkingAllTheWay)
      .mapToDouble(Itinerary::distanceMeters)
      .min();

    if (walkDistance.isEmpty()) {
      return filtered;
    }
    final double walkLimit = walkDistance.getAsDouble();
    List<Itinerary> filtered2 = itineraries
      .stream()
      .filter(it -> getWalkDistance(it) > walkLimit)
      .collect(Collectors.toList());

    // remove duplicates
    filtered2.removeAll(filtered);
    filtered.addAll(filtered2);

    return filtered;
  }

  @Override
  public boolean skipAlreadyFlaggedItineraries() {
    return false;
  }
}
