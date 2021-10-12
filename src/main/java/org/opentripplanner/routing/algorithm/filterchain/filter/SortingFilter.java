package org.opentripplanner.routing.algorithm.filterchain.filter;

import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.routing.algorithm.filterchain.ItineraryListFilter;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * This is a filter to sort itineraries. To create a filter, provide a comparator as a constructor
 * argument.
 */
public final class SortingFilter implements ItineraryListFilter {

    private final Comparator<Itinerary> comparator;

    public SortingFilter(Comparator<Itinerary> comparator) {
        this.comparator = comparator;
    }

    public Comparator<Itinerary> comparator() {
        return comparator;
    }

    @Override
    public List<Itinerary> filter(List<Itinerary> itineraries) {
        if (itineraries.size() < 2) {
            return itineraries;
        }
        // Sort acceding by qualifier and map to list of itineraries
        return itineraries.stream()
            .sorted(comparator())
            .collect(Collectors.toList());
    }
}
