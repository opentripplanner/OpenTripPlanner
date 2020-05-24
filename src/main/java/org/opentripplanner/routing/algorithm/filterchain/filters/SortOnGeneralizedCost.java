package org.opentripplanner.routing.algorithm.filterchain.filters;

import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.routing.algorithm.filterchain.ItineraryFilter;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * This filter sort itineraries based on the generalized cost plus a cost for all transfers. The
 * cost of transfers are added, because this is not part of the cost function in the raptor search.
 * <p>
 * We tried different ways to do this. Later when we have more/cleaner criteria in the raptor search
 * this filter could be exchanged with another better filter, like using a pareto-set and a spread
 * function, or sorting on the length of the weighted normalized vector of all criteria.
 */
public class SortOnGeneralizedCost implements ItineraryFilter {

    private final int transferCost;

    public SortOnGeneralizedCost(int transferCost) {
        this.transferCost = transferCost;
    }

    @Override
    public String name() {
        return "sort-on-generalized-cost";
    }

    @Override
    public List<Itinerary> filter(List<Itinerary> itineraries) {
        if (itineraries.size() < 2) {
            return itineraries;
        }
        // Sort acceding by qualifier and map to list of itineraries
        return itineraries
                .stream()
                .sorted(Comparator.comparingDouble(this::calculateQualifier))
                .collect(Collectors.toList());
    }

    private int calculateQualifier(Itinerary it) {
        return it.generalizedCost + transferCost * it.nTransfers;
    }
}
