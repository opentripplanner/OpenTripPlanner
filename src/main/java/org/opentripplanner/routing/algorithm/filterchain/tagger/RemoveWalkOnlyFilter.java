package org.opentripplanner.routing.algorithm.filterchain.tagger;

import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.routing.algorithm.filterchain.ItineraryListFilter;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Filter itineraries and remove all itineraries where all legs is walking.
 */
public class RemoveWalkOnlyFilter implements ItineraryTagger {

    @Override
    public String name() {
        return "remove-walk-only-filter";
    }

    @Override
    public boolean filterUntaggedItineraries() {
        return true;
    }

    @Override
    public void tagItineraries(List<Itinerary> itineraries) {
        itineraries.stream()
            .filter(Itinerary::isWalkingAllTheWay)
            .forEach(it -> it.markAsDeleted(notice()));
    }
}
