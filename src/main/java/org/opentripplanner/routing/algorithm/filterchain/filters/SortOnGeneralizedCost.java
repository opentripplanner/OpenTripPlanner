package org.opentripplanner.routing.algorithm.filterchain.filters;

import org.opentripplanner.util.CompositeComparator;

/**
 * This filter sort itineraries based on the generalized-cost. If the cost is the same then
 * the filter pick the itinerary with the lowest number-of-transfers.
 */
public class SortOnGeneralizedCost extends SortFilter {

    public SortOnGeneralizedCost() {
        super(
            new CompositeComparator<>(
                OtpDefaultSortOrder.GENERALIZED_COST,
                OtpDefaultSortOrder.NUM_OF_TRANSFERS
            )
        );
    }

    @Override
    public String name() {
        return "sort-on-generalized-cost";
    }
}
