package org.opentripplanner.routing.algorithm.filterchain.deletionflagger;

import java.util.List;
import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.model.plan.Leg;
import org.opentripplanner.routing.algorithm.filterchain.ItineraryListFilter;

/**
 * This filter is useful if you want to remove
 */
public class RemoveBikeParkWithShortBikeFilter implements ItineraryListFilter {

  private final double minCyclingToTransitDistance;

  public RemoveBikeParkWithShortBikeFilter(double minCyclingToTransitDistance) {
    this.minCyclingToTransitDistance = minCyclingToTransitDistance;
  }

  @Override
  public List<Itinerary> filter(List<Itinerary> itineraries) {
    return itineraries.stream().filter(this::filterItinerariesWithShortBikeParkingLeg).toList();
  }

  private boolean filterItinerariesWithShortBikeParkingLeg(Itinerary itinerary) {
    var hasCyclingLegs = itinerary.getLegs().stream().anyMatch(Leg::isCyclingLeg);
    if (hasCyclingLegs && itinerary.hasTransit()) {
      double bikeParkingDistance = 0;
      for (var leg : itinerary.getLegs()) {
        if (leg.isCyclingLeg()) {
          bikeParkingDistance += leg.getDistanceMeters();
        }
      }

      return bikeParkingDistance > minCyclingToTransitDistance;
    } else {
      return true;
    }
  }
}
