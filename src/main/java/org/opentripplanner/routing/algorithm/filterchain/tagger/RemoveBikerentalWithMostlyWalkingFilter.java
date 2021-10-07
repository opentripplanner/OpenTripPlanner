package org.opentripplanner.routing.algorithm.filterchain.tagger;

import java.util.ArrayList;
import java.util.List;
import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.routing.algorithm.filterchain.ItineraryListFilter;

/**
 * This is used to filter out bike rental itineraries that contain mostly walking. The value
 * describes the ratio of the total itinerary that has to consist of bike rental to allow the
 * itinerary.
 * <p>
 * This filter is turned off by default (bikeRentalDistanceRatio == 0)
 */
public class RemoveBikerentalWithMostlyWalkingFilter implements ItineraryTagger {

    private final double bikeRentalDistanceRatio;

    public RemoveBikerentalWithMostlyWalkingFilter(double bikeRentalDistanceRatio) {
        this.bikeRentalDistanceRatio = bikeRentalDistanceRatio;
    }

    @Override
    public String name() {
        return "bikerental-vs-walk-filter";
    }

    @Override
    public void tagItineraries(List<Itinerary> itineraries) {
        for (Itinerary itinerary : itineraries) {
            var containsTransit =
                    itinerary.legs.stream().anyMatch(l -> l != null && l.mode.isTransit());

            double bikeRentalDistance = itinerary.legs
                    .stream()
                    .filter(l -> l.rentedVehicle != null && l.rentedVehicle)
                    .mapToDouble(l -> l.distanceMeters)
                    .sum();
            double totalDistance = itinerary.distanceMeters();

            if (bikeRentalDistance != 0
                    && !containsTransit
                    && (bikeRentalDistance / totalDistance) <= bikeRentalDistanceRatio) {
                itinerary.markAsDeleted(notice());
            }
        }
    }
}
