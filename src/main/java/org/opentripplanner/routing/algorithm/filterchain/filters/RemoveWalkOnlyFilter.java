package org.opentripplanner.routing.algorithm.filterchain.filters;

import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.routing.algorithm.filterchain.ItineraryListFilter;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Filter itineraries and remove all itineraries where all legs is walking.
 */
public class RemoveWalkOnlyFilter implements ItineraryListFilter {

    @Override
    public String name() {
        return "remove-walk-only-filter";
    }

    @Override
    public List<Itinerary> filter(List<Itinerary> itineraries) {
        return itineraries
            .stream().filter(it -> !it.isWalkingAllTheWay())
            .collect(Collectors.toList());
    }

    @Override
    public boolean removeItineraries() {
        return true;
    }
}
