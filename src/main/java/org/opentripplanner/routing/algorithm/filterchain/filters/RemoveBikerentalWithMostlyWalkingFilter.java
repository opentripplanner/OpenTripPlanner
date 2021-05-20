package org.opentripplanner.routing.algorithm.filterchain.filters;

import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.routing.algorithm.filterchain.ItineraryFilter;

import java.util.ArrayList;
import java.util.List;

/**
 * This is used to filter out bike rental itineraries that contain mostly walking. The value
 * describes the ratio of the total itinerary that has to consist of bike rental to allow the
 * itinerary.
 * 
 * This filter is turned off by default (bikeRentalDistanceRatio == 0)
 */
public class RemoveBikerentalWithMostlyWalkingFilter implements ItineraryFilter {

  private final double bikeRentalDistanceRatio;

  public RemoveBikerentalWithMostlyWalkingFilter(double bikeRentalDistanceRatio) {
    this.bikeRentalDistanceRatio = bikeRentalDistanceRatio;
  }

  @Override
  public String name() {
    return "bikerental-vs-walk-filter";
  }

  @Override
  public List<Itinerary> filter(List<Itinerary> itineraries) {
    List<Itinerary> result = new ArrayList<>();

    for (Itinerary itinerary : itineraries) {
      double bikeRentalDistance = itinerary.legs
          .stream()
          .filter(l -> l.rentedBike != null && l.rentedBike)
          .mapToDouble(l -> l.distanceMeters)
          .sum();
      double totalDistance = itinerary.distanceMeters();

      if (bikeRentalDistance == 0
          || (bikeRentalDistance / totalDistance) > bikeRentalDistanceRatio) {
        result.add(itinerary);
      }
    }
    return result;
  }

  @Override
  public boolean removeItineraries() {
    return true;
  }
}
