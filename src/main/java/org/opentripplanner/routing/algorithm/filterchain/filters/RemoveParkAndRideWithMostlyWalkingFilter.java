package org.opentripplanner.routing.algorithm.filterchain.filters;

import java.util.ArrayList;
import java.util.List;
import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.model.plan.Leg;
import org.opentripplanner.routing.algorithm.filterchain.ItineraryFilter;
import org.opentripplanner.routing.core.TraverseMode;

/**
 * This is used to filter out bike rental itineraries that contain mostly walking. The value
 * describes the ratio of the total itinerary that has to consist of bike rental to allow the
 * itinerary.
 * <p>
 * This filter is turned off by default (parkAndRideDurationRatio == 0)
 */
public class RemoveParkAndRideWithMostlyWalkingFilter implements ItineraryFilter {

    private final double parkAndRideDurationRatio;

    public RemoveParkAndRideWithMostlyWalkingFilter(double ratio) {
        this.parkAndRideDurationRatio = ratio;
    }

    @Override
    public String name() {
        return "park-and-ride-vs-walk-filter";
    }

    @Override
    public List<Itinerary> filter(List<Itinerary> itineraries) {
        List<Itinerary> result = new ArrayList<>();

        for (Itinerary itinerary : itineraries) {
            var containsTransit =
                    itinerary.legs.stream().anyMatch(l -> l != null && l.mode.isTransit());

            double carDuration = itinerary.legs
                    .stream()
                    .filter(l -> l.mode == TraverseMode.CAR)
                    .mapToDouble(Leg::getDuration)
                    .sum();
            double totalDuration = itinerary.durationSeconds;

            if (containsTransit
                    || itineraries.size() == 1
                    || carDuration == 0
                    || (carDuration / totalDuration) > parkAndRideDurationRatio) {
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
