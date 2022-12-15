package org.opentripplanner.routing.algorithm.filterchain.deletionflagger;

import java.util.List;
import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.model.plan.Leg;
import org.opentripplanner.routing.algorithm.filterchain.ItineraryListFilter;
import org.opentripplanner.street.search.TraverseMode;

/**
 * This filter is useful if you want to remove those results where there is a short bicycle
 * leg followed by parking the bike and taking transit. In such a case you would not need a bike
 * could just walk to the stop instead.
 */
public class RemoveItinerariesWithShortStreetLeg implements ItineraryListFilter {

  private final double minDistance;
  private final TraverseMode traverseMode;

  public RemoveItinerariesWithShortStreetLeg(double minDistance, TraverseMode traverseMode) {
    this.minDistance = minDistance;
    this.traverseMode = traverseMode;
  }

  @Override
  public List<Itinerary> filter(List<Itinerary> itineraries) {
    return itineraries.stream().filter(this::filterItinerariesWithShortStreetLeg).toList();
  }

  private boolean filterItinerariesWithShortStreetLeg(Itinerary itinerary) {
    var hasLegsOfMode = itinerary.getStreetLegs().anyMatch(l -> l.getMode().equals(traverseMode));
    if (hasLegsOfMode && itinerary.hasTransit()) {
      var distance = itinerary
        .getStreetLegs()
        .filter(l -> l.getMode().equals(traverseMode))
        .mapToDouble(Leg::getDistanceMeters)
        .sum();

      return distance > minDistance;
    } else {
      return true;
    }
  }
}
