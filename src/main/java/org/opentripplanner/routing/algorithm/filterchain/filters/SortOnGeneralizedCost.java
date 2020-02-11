package org.opentripplanner.routing.algorithm.filterchain.filters;

import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.routing.algorithm.filterchain.ItineraryFilter;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class SortOnGeneralizedCost implements ItineraryFilter {
    private final int transferCost;

    public SortOnGeneralizedCost(int transferCost) {
        this.transferCost = transferCost;
    }

    @Override
    public String name() {
        return "generalized-cost-filter";
    }

    @Override
    public List<Itinerary> filter(List<Itinerary> itineraries) {
        if(itineraries.size() < 2) {
            return itineraries;
        }
        // Make a list of itineraries with qualifier
        List<ItineraryWrapper> list = new ArrayList<>();

        for (Itinerary it : itineraries) {
            double q = it.generalizedCost + transferCost * it.nTransfers;
            list.add(new ItineraryWrapper(it, q));
        }

        // Sort acceding by qualifier and map to list of itineraries
        return list.stream()
                .sorted(Comparator.comparingDouble(l -> l.q))
                .map(it -> it.itinerary)
                .collect(Collectors.toList());
    }

    private static class ItineraryWrapper {
        final Itinerary itinerary;
        final double q;

        public ItineraryWrapper(Itinerary itinerary, double q) {
            this.itinerary = itinerary;
            this.q = q;
        }
    }
}
