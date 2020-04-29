package org.opentripplanner.routing.algorithm.filterchain.filters;

import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.routing.algorithm.filterchain.ItineraryFilter;

import java.util.List;

public class FilterChain implements ItineraryFilter {
    private final List<ItineraryFilter> filters;

    public FilterChain(List<ItineraryFilter> filters) {
        this.filters = filters;
    }

    @Override
    public String name() {
        return "filter-chain";
    }

    @Override
    public List<Itinerary> filter(List<Itinerary> itineraries) {
        List<Itinerary> result = itineraries;
        for (ItineraryFilter filter : filters) {
            result = filter.filter(result);
        }
        return result;
    }
}
