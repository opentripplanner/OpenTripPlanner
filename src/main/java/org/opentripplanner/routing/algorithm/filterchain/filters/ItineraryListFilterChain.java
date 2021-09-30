package org.opentripplanner.routing.algorithm.filterchain.filters;

import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.routing.algorithm.filterchain.ItineraryListFilter;

import java.util.List;

public class ItineraryListFilterChain {
    private final List<ItineraryListFilter> filters;

    public ItineraryListFilterChain(List<ItineraryListFilter> filters) {
        this.filters = filters;
    }

    public List<Itinerary> filter(List<Itinerary> itineraries) {
        List<Itinerary> result = itineraries;
        for (ItineraryListFilter filter : filters) {
            result = filter.filter(result);
        }
        return result;
    }
}
