package org.opentripplanner.routing.algorithm.filterchain.filters;

import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.routing.algorithm.filterchain.ItineraryFilter;
import org.opentripplanner.util.CompositeComparator;

import java.util.List;
import java.util.stream.Collectors;

/**
 * This filter sort itineraries based on the generalized-cost. If the cost is the same then
 * the filter pick the itinerary with the lowest number-of-transfers.
 */
public class SortOnGeneralizedCost
    extends CompositeComparator<Itinerary>
    implements ItineraryFilter
{
    public SortOnGeneralizedCost() {
        super(
            OtpDefaultSortOrder.GENERALIZED_COST,
            OtpDefaultSortOrder.NUM_OF_TRANSFERS
        );
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
        return itineraries.stream().sorted(this).collect(Collectors.toList());
    }

    @Override
    public boolean removeItineraries() { return false; }
}
