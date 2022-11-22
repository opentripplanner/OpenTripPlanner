package org.opentripplanner.routing.algorithm.filterchain.deletionflagger;

import java.util.List;
import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.model.plan.Leg;
import org.opentripplanner.routing.algorithm.filterchain.ItineraryListFilter;

/**
 * This filter is useful if you want to remove
 */
public class RemoveBikeParkWithShortBikeFilter implements ItineraryListFilter {

  private final double minBikeParkingDistance;

  public RemoveBikeParkWithShortBikeFilter(double minBikeParkingDistance) {
    this.minBikeParkingDistance = minBikeParkingDistance;
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

      return bikeParkingDistance > minBikeParkingDistance;
    } else {
      return true;
    }
  }
}
