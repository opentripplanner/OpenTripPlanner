package org.opentripplanner.routing.algorithm.filterchain;

import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.routing.algorithm.filterchain.ItineraryListFilter;

import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class ItineraryListFilterChain {
    private final List<ItineraryListFilter> filters;
    private final boolean debug;

    public ItineraryListFilterChain(List<ItineraryListFilter> filters, boolean debug) {
        this.filters = filters;
        this.debug = debug;
    }

    public List<Itinerary> filter(List<Itinerary> itineraries) {
        List<Itinerary> result = itineraries;
        for (ItineraryListFilter filter : filters) {
            result = filter.filter(result);
        }

        if (debug) {
          return result;
        }  
        return result.stream()
                .filter(Predicate.not(Itinerary::isMarkedAsDeleted))
                .collect(Collectors.toList());
    }
}
