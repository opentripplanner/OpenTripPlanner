package org.opentripplanner.routing.algorithm.filterchain.deletionflagger;

import java.util.function.Predicate;
import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.model.plan.Leg;

/**
 * This is used to filter out bike rental itineraries that contain mostly walking. The value
 * describes the ratio of the total itinerary that has to consist of bike rental to allow the
 * itinerary.
 * <p>
 * This filter is turned off by default (bikeRentalDistanceRatio == 0)
 */
public class RemoveBikerentalWithMostlyWalkingFilter implements ItineraryDeletionFlagger {

  private final double bikeRentalDistanceRatio;

  public RemoveBikerentalWithMostlyWalkingFilter(double bikeRentalDistanceRatio) {
    this.bikeRentalDistanceRatio = bikeRentalDistanceRatio;
  }

  @Override
  public String name() {
    return "bikerental-vs-walk-filter";
  }

  @Override
  public Predicate<Itinerary> shouldBeFlaggedForRemoval() {
    return itinerary -> {
      var containsTransit = itinerary
        .getLegs()
        .stream()
        .anyMatch(l -> l != null && l.isTransitLeg());

      double bikeRentalDistance = itinerary
        .getLegs()
        .stream()
        .filter(l -> l.getRentedVehicle() != null && l.getRentedVehicle())
        .mapToDouble(Leg::getDistanceMeters)
        .sum();
      double totalDistance = itinerary.distanceMeters();

      return (
        bikeRentalDistance != 0 &&
        !containsTransit &&
        (bikeRentalDistance / totalDistance) <= bikeRentalDistanceRatio
      );
    };
  }
}
