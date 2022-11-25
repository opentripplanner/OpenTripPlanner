package org.opentripplanner.routing.algorithm.filterchain.deletionflagger;

import java.util.List;
import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.model.plan.Leg;
import org.opentripplanner.routing.algorithm.filterchain.ItineraryListFilter;

/**
 * This filter is useful if you want to remove those results where there is a short bicycle
 * leg followed by parking the bike and taking transit. In such a case you would not need a bike
 * could just walk to the stop instead.
 */
public class RemoveItinerariesWithShortCyclingLeg implements ItineraryListFilter {

  private final double minCyclingDistance;

  public RemoveItinerariesWithShortCyclingLeg(double minCyclingDistance) {
    this.minCyclingDistance = minCyclingDistance;
  }

  @Override
  public List<Itinerary> filter(List<Itinerary> itineraries) {
    return itineraries.stream().filter(this::filterItinerariesWithShortCyclingLeg).toList();
  }

  private boolean filterItinerariesWithShortCyclingLeg(Itinerary itinerary) {
    var hasCyclingLegs = itinerary.getLegs().stream().anyMatch(Leg::isCyclingLeg);
    if (hasCyclingLegs && itinerary.hasTransit()) {
      double cylingDistance = 0;
      for (var leg : itinerary.getLegs()) {
        if (leg.isCyclingLeg()) {
          cylingDistance += leg.getDistanceMeters();
        }
      }

      return cylingDistance > minCyclingDistance;
    } else {
      return true;
    }
  }
}
