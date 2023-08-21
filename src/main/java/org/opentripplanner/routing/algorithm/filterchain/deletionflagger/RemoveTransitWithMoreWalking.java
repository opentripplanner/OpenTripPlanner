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
 * Filter itineraries which contain more walking than a pure walk itinerary
 */
public class RemoveTransitWithMoreWalking implements ItineraryDeletionFlagger {

  /**
   * Required for {@link org.opentripplanner.routing.algorithm.filterchain.ItineraryListFilterChain},
   * to know which filters removed
   */
  public static final String TAG = "transit-vs-plain-walk-filter";

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
    // Filter the most common silly itinerary case: transit itinerary has more walking than plain walk itinerary
    // This never makes sense
    OptionalDouble walkDistance = itineraries
      .stream()
      .filter(Itinerary::isWalkingAllTheWay)
      .mapToDouble(Itinerary::distanceMeters)
      .min();

    if (walkDistance.isEmpty()) {
      return List.of();
    }

    final double walkLimit = walkDistance.getAsDouble();
    return itineraries
      .stream()
      .filter(it -> !it.isOnStreetAllTheWay() && getWalkDistance(it) > walkLimit)
      .collect(Collectors.toList());
  }

  @Override
  public boolean skipAlreadyFlaggedItineraries() {
    return false;
  }
}
