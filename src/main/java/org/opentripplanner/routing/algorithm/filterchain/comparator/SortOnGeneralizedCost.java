package org.opentripplanner.routing.algorithm.filterchain.comparator;

import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.util.CompositeComparator;

/**
 * This comparator sorts itineraries based on the generalized-cost. If the cost is the same then
 * the filter pick the itinerary with the lowest number-of-transfers.
 */
public class SortOnGeneralizedCost extends CompositeComparator<Itinerary> {

    public SortOnGeneralizedCost() {
        super(
            OtpDefaultSortOrder.GENERALIZED_COST,
            OtpDefaultSortOrder.NUM_OF_TRANSFERS
        );
    }
}
