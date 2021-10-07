package org.opentripplanner.routing.algorithm.filterchain.tagger;

import java.util.ArrayList;
import java.util.List;
import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.model.plan.Leg;
import org.opentripplanner.routing.algorithm.filterchain.ItineraryListFilter;
import org.opentripplanner.routing.core.TraverseMode;

/**
 * This is used to filter out bike rental itineraries that contain mostly walking. The value
 * describes the ratio of the total itinerary that has to consist of bike rental to allow the
 * itinerary.
 * <p>
 * This filter is turned off by default (parkAndRideDurationRatio == 0)
 */
public class RemoveParkAndRideWithMostlyWalkingFilter implements ItineraryTagger {

    private final double parkAndRideDurationRatio;

    public RemoveParkAndRideWithMostlyWalkingFilter(double ratio) {
        this.parkAndRideDurationRatio = ratio;
    }

    @Override
    public String name() {
        return "park-and-ride-vs-walk-filter";
    }

    @Override
    public void tagItineraries(List<Itinerary> itineraries) {
        if (itineraries.size() == 1) { return; }

        for (Itinerary itinerary : itineraries) {
            var containsTransit =
                    itinerary.legs.stream().anyMatch(l -> l != null && l.mode.isTransit());

            double carDuration = itinerary.legs
                    .stream()
                    .filter(l -> l.mode == TraverseMode.CAR)
                    .mapToDouble(Leg::getDuration)
                    .sum();
            double totalDuration = itinerary.durationSeconds;

            if (!containsTransit
                    && carDuration != 0
                    && (carDuration / totalDuration) <= parkAndRideDurationRatio) {
                itinerary.markAsDeleted(notice());
            }
        }
    }
}
