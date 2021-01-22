package org.opentripplanner.routing.algorithm.filterchain.filters;

import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.routing.algorithm.filterchain.ItineraryFilter;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * This is an abstract filter to sort itineraries. To create a sub-class, implement the
 * {@link ItineraryFilter#name()} and provide a comparator. You may pass in the comparator
 * as a constructor argument or override the {@link #comparator()} method.
 */
public abstract class SortFilter implements ItineraryFilter {

    private final Comparator<Itinerary> comparator;

    public SortFilter() {
        this(null);
    }

    public SortFilter(Comparator<Itinerary> comparator) {
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

    @Override
    public final boolean removeItineraries() { return false; }
}
