package org.opentripplanner.routing.algorithm.filterchain.filters;

import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.routing.algorithm.filterchain.ItineraryFilter;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Filter itineraries based on duration, compared with a walk-all-the-way itinerary(if it exist).
 * If an itinerary is not faster than the walk-all-the-way minus a given slack, then the transit
 * itinerary is removed.
 *
 */
public class RemoveTransitIfStreetOnlyIsBetterFilter implements ItineraryFilter {

    @Override
    public String name() {
        return "transit-vs-street-filter";
    }

    @Override
    public List<Itinerary> filter(List<Itinerary> itineraries) {
        // Find the best walk-all-the-way option
        Optional<Itinerary> bestStreetOp = itineraries
                .stream()
            .filter(Itinerary::isOnStreetAllTheWay)
            .min(Comparator.comparingInt(l -> l.generalizedCost));

        if(bestStreetOp.isEmpty()) {
            return itineraries;
        }

        final long limit = bestStreetOp.get().generalizedCost;

        // Filter away itineraries that have higher cost than the best non-transit option.
        return itineraries.stream()
                .filter( it -> it.isOnStreetAllTheWay() || it.generalizedCost < limit)
                .collect(Collectors.toList());
    }

    @Override
    public boolean removeItineraries() {
        return true;
    }
}
