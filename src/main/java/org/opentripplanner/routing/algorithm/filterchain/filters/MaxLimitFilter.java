package org.opentripplanner.routing.algorithm.filterchain.filters;

import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.routing.algorithm.filterchain.ItineraryFilter;

import java.util.List;
import java.util.stream.Collectors;

public class MaxLimitFilter implements ItineraryFilter {
    private final String name;
    private final int maxLimit;

    public MaxLimitFilter(String name, int maxLimit) {
        this.name = name;
        this.maxLimit = maxLimit;
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public List<Itinerary> filter(final List<Itinerary> itineraries) {
        if(itineraries.isEmpty()) { return itineraries; }

        // Count elements(i) and elements not marked for deletion(j)
        int i=0, j=0;

        while (i < itineraries.size() && j < maxLimit) {
            Itinerary it = itineraries.get(i);
            if(!it.debugMarkedAsDeleted) { ++j; }
            ++i;
        }
        return itineraries.stream().limit(i).collect(Collectors.toList());
    }
}
