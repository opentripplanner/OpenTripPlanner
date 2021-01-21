package org.opentripplanner.routing.algorithm.filterchain.filters;

import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.routing.algorithm.filterchain.AdjustedCost;
import org.opentripplanner.util.CompositeComparator;

import java.util.Comparator;
import java.util.List;

import static java.util.Comparator.comparingInt;
import static org.opentripplanner.routing.algorithm.filterchain.filters.OtpDefaultSortOrder.ARRIVAL_TIME;
import static org.opentripplanner.routing.algorithm.filterchain.filters.OtpDefaultSortOrder.DEPARTURE_TIME;

/**
 * This filter sort itineraries based on a adjusted cost. If the adjusted-cost is
 * calculated using the generalized-cost plus a cost for short transfer times.
 *
 * @see AdjustedCost
 */
public class SortOnAdjustedCost extends SortFilter {
    private final boolean arriveBy;
    private final AdjustedCost adjustedCost;

    private int minSafeTransferTime = 0;


    public SortOnAdjustedCost(
        boolean arriveBy,
        AdjustedCost adjustedCost
    ) {
        this.arriveBy = arriveBy;
        this.adjustedCost = adjustedCost;
    }

    @Override
    public List<Itinerary> filter(List<Itinerary> itineraries) {
        this.minSafeTransferTime = adjustedCost.minSafeTransferTime(itineraries);
        return super.filter(itineraries);
    }

    @Override
    public String name() {
        return "sort-on-adjusted-cost";
    }

    @Override
    public Comparator<Itinerary> comparator() {
        return new CompositeComparator<>(
            comparingInt(i -> adjustedCost.calculate(minSafeTransferTime, i)),
            arriveBy ? DEPARTURE_TIME : ARRIVAL_TIME
        );
    }
}
