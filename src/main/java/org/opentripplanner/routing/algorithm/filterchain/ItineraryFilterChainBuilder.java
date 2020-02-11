package org.opentripplanner.routing.algorithm.filterchain;

import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.routing.algorithm.filterchain.filters.DebugFilterChain;
import org.opentripplanner.routing.algorithm.filterchain.filters.FilterChain;
import org.opentripplanner.routing.algorithm.filterchain.filters.GroupByFilter;
import org.opentripplanner.routing.algorithm.filterchain.filters.LatestDepartureTimeFilter;
import org.opentripplanner.routing.algorithm.filterchain.filters.LongTransitWalkingFilter;
import org.opentripplanner.routing.algorithm.filterchain.filters.MaxLimitFilter;
import org.opentripplanner.routing.algorithm.filterchain.filters.SortOnGeneralizedCost;
import org.opentripplanner.routing.algorithm.filterchain.filters.SortOnWalkingArrivalAndDeparture;
import org.opentripplanner.routing.algorithm.filterchain.groupids.GroupByLongestLegsId;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;


/**
 * Create a filter chain based on the given config.
 */
public class ItineraryFilterChainBuilder {
    private double groupByP = 0.68;
    private int minLimit = 3;
    private int maxLimit = 20;
    private int groupByTransferCost = 10 * 60;
    private Instant latestDepartureTimeLimit = null;
    private boolean debug;

    /** Max departure time in epoch seconds */
    public void setLatestDepartureTimeLimit(Instant latestDepartureTimeLimit) {
        this.latestDepartureTimeLimit = latestDepartureTimeLimit;
    }

    /**
     * Set the minimum filter limit. The filtering is skipped if the number of itineraries
     * is below or equal to the min limit.
     */
    public void setMinLimit(int minLimit) {
        this.minLimit = minLimit;
    }

    /**
     * The maximum number of itineraries returned. This will remove all itineraries at the
     * end of the list, just before the filter chain returns - this is the last step.
     */
    public void setMaxLimit(int maxLimit) {
        this.maxLimit = maxLimit;
    }

    /**
     * Group by legs that account for more then 'p' % for the total distance.
     * Must be a number between 0.0 (0%) and 1.0 (100%).
     */
    public void setGroupByP(double groupByP) {
        this.groupByP = groupByP;
    }

    /**
     * The group-by-legs filter filter each group based on 'generalized-cost' and
     * 'transfer-cost'. The unit is equivalent to the cost of "one transit second".
     */
    public void setGroupByTransferCost(int cost) {
        groupByTransferCost = cost;
    }

    /**
     * This will NOT delete itineraries, but mark them as deleted using the
     * {@link Itinerary#markAsDeleted()} and add an alert to the first leg and
     * the second leg (if first leg is walking).
     */
    public void debug() {
        this.debug = true;
    }

    public ItineraryFilter build() {
        final List<ItineraryFilter> filters = new ArrayList<>();

        filters.add(new LongTransitWalkingFilter());

        filters.add(new GroupByFilter<>(
                "groupBy-legs-filter-on-cost",
                it -> new GroupByLongestLegsId(it, groupByP),
                new SortOnGeneralizedCost(groupByTransferCost),
                minLimit
        ));

        // A Range Raptor search produces
        if (latestDepartureTimeLimit != null) {
            filters.add(new LatestDepartureTimeFilter(latestDepartureTimeLimit));
        }

        // Sort itineraries
        filters.add(new SortOnWalkingArrivalAndDeparture());

        // Remove itineraries if max limit is exceeded
        if (maxLimit > minLimit) {
            filters.add(new MaxLimitFilter("MAX", maxLimit));
        }

        return debug ? new DebugFilterChain(filters) : new FilterChain(filters);
    }
}
