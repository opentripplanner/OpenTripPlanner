package org.opentripplanner.routing.algorithm.filterchain.deletionflagger;

import java.util.function.Predicate;
import org.opentripplanner.model.plan.Itinerary;

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
    public Predicate<Itinerary> predicate() {
        return itinerary -> {
            var containsTransit =
                    itinerary.legs.stream().anyMatch(l -> l != null && l.getMode().isTransit());

            double bikeRentalDistance = itinerary.legs
                    .stream()
                    .filter(l -> l.getRentedVehicle() != null && l.getRentedVehicle())
                    .mapToDouble(l -> l.getDistanceMeters())
                    .sum();
            double totalDistance = itinerary.distanceMeters();

            return bikeRentalDistance != 0
                    && !containsTransit
                    && (bikeRentalDistance / totalDistance) <= bikeRentalDistanceRatio;
        };
    }
}

