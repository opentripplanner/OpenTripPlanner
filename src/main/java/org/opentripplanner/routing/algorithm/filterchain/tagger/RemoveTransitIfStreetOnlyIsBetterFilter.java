package org.opentripplanner.routing.algorithm.filterchain.tagger;

import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.routing.algorithm.filterchain.ItineraryListFilter;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Filter itineraries based on generalizedCost, compared with a on-street-all-the-way itinerary(if
 * it exist). If an itinerary is slower than the best all-the-way-on-street itinerary, then the
 * transit itinerary is removed.
 */
public class RemoveTransitIfStreetOnlyIsBetterFilter implements ItineraryTagger {

    @Override
    public String name() {
        return "transit-vs-street-filter";
    }

    @Override
    public void tagItineraries(List<Itinerary> itineraries) {
        // Find the best walk-all-the-way option
        Optional<Itinerary> bestStreetOp = itineraries.stream()
            .filter(Itinerary::isOnStreetAllTheWay)
            .min(Comparator.comparingInt(l -> l.generalizedCost));

        if(bestStreetOp.isEmpty()) {
            return;
        }

        final long limit = bestStreetOp.get().generalizedCost;

        // Filter away itineraries that have higher cost than the best non-transit option.
        itineraries.stream()
            .filter( it -> !it.isOnStreetAllTheWay() && it.generalizedCost >= limit)
            .forEach(it -> it.markAsDeleted(notice()));
    }
}
