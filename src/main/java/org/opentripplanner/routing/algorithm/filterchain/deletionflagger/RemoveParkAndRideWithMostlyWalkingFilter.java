package org.opentripplanner.routing.algorithm.filterchain.deletionflagger;

import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.model.plan.Leg;
import org.opentripplanner.routing.core.TraverseMode;

/**
 * This is used to filter out bike rental itineraries that contain mostly walking. The value
 * describes the ratio of the total itinerary that has to consist of bike rental to allow the
 * itinerary.
 * <p>
 * This filter is turned off by default (parkAndRideDurationRatio == 0)
 */
public class RemoveParkAndRideWithMostlyWalkingFilter implements ItineraryDeletionFlagger {

    private final double parkAndRideDurationRatio;

    public RemoveParkAndRideWithMostlyWalkingFilter(double ratio) {
        this.parkAndRideDurationRatio = ratio;
    }

    @Override
    public String name() {
        return "park-and-ride-vs-walk-filter";
    }

    @Override
    public Predicate<Itinerary> predicate() {
        return itinerary -> {
            var containsTransit =
                    itinerary.legs.stream().anyMatch(l -> l != null && l.getMode().isTransit());

            double carDuration = itinerary.legs
                    .stream()
                    .filter(l -> l.getMode() == TraverseMode.CAR)
                    .mapToDouble(Leg::getDuration)
                    .sum();
            double totalDuration = itinerary.durationSeconds;

            return !containsTransit
                    && carDuration != 0
                    && (carDuration / totalDuration) <= parkAndRideDurationRatio;
        };
    }

    @Override
    public List<Itinerary> getFlaggedItineraries(List<Itinerary> itineraries) {
        if (itineraries.size() == 1) { return List.of(); }

        return itineraries.stream().filter(predicate()).collect(Collectors.toList());
    }
}
