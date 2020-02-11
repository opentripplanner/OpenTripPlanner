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

        return itineraries.stream().limit(maxLimit).collect(Collectors.toList());
    }
}
