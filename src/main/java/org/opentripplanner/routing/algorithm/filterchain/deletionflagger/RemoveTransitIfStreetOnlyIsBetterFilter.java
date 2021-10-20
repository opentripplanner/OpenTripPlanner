package org.opentripplanner.routing.algorithm.filterchain.deletionflagger;

import java.util.stream.Collectors;
import org.opentripplanner.model.plan.Itinerary;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Filter itineraries based on generalizedCost, compared with a on-street-all-the-way itinerary(if
 * it exist). If an itinerary is slower than the best all-the-way-on-street itinerary, then the
 * transit itinerary is removed.
 */
public class RemoveTransitIfStreetOnlyIsBetterFilter implements ItineraryDeletionFlagger {

    @Override
    public String name() {
        return "transit-vs-street-filter";
    }

    @Override
    public List<Itinerary> getFlaggedItineraries(List<Itinerary> itineraries) {
        // Find the best walk-all-the-way option
        Optional<Itinerary> bestStreetOp = itineraries.stream()
            .filter(Itinerary::isOnStreetAllTheWay)
            .min(Comparator.comparingInt(l -> l.generalizedCost));

        if(bestStreetOp.isEmpty()) {
            return List.of();
        }

        final long limit = bestStreetOp.get().generalizedCost;

        // Filter away itineraries that have higher cost than the best non-transit option.
        return itineraries.stream()
            .filter( it -> !it.isOnStreetAllTheWay() && it.generalizedCost >= limit)
            .collect(Collectors.toList());
    }
}
