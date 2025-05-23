package org.opentripplanner.routing.algorithm.filterchain.filters.street;

import java.util.function.Predicate;
import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.model.plan.Leg;
import org.opentripplanner.routing.algorithm.filterchain.framework.spi.RemoveItineraryFlagger;

/**
 * This is used to filter out bike rental itineraries that contain mostly walking. The value
 * describes the ratio of the total itinerary that has to consist of bike rental to allow the
 * itinerary.
 * <p>
 * This filter is turned off by default (bikeRentalDistanceRatio == 0)
 */
public class RemoveBikeRentalWithMostlyWalking implements RemoveItineraryFlagger {

  private final double bikeRentalDistanceRatio;

  public RemoveBikeRentalWithMostlyWalking(double bikeRentalDistanceRatio) {
    this.bikeRentalDistanceRatio = bikeRentalDistanceRatio;
  }

  @Override
  public String name() {
    return "bikerental-vs-walk-filter";
  }

  @Override
  public Predicate<Itinerary> shouldBeFlaggedForRemoval() {
    return itinerary -> {
      if (itinerary.hasTransit()) {
        return false;
      }

      double bikeRentalDistance = itinerary
        .legs()
        .stream()
        .filter(l -> l.rentedVehicle() != null && l.rentedVehicle())
        .mapToDouble(Leg::distanceMeters)
        .sum();

      double totalDistance = itinerary.distanceMeters();
      return (
        bikeRentalDistance != 0 && (bikeRentalDistance / totalDistance) <= bikeRentalDistanceRatio
      );
    };
  }
}
