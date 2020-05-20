package org.opentripplanner.routing.algorithm.filterchain;

import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.routing.algorithm.filterchain.filters.DebugFilterWrapper;
import org.opentripplanner.routing.algorithm.filterchain.filters.FilterChain;
import org.opentripplanner.routing.algorithm.filterchain.filters.GroupByFilter;
import org.opentripplanner.routing.algorithm.filterchain.filters.LatestDepartureTimeFilter;
import org.opentripplanner.routing.algorithm.filterchain.filters.LongTransitWalkingFilter;
import org.opentripplanner.routing.algorithm.filterchain.filters.MaxLimitFilter;
import org.opentripplanner.routing.algorithm.filterchain.filters.OtpDefaultSortOrder;
import org.opentripplanner.routing.algorithm.filterchain.filters.SortOnGeneralizedCost;
import org.opentripplanner.routing.algorithm.filterchain.groupids.GroupByLongestLegsId;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;


/**
 * Create a filter chain based on the given config.
 */
public class ItineraryFilterChainBuilder {
    private final boolean arriveBy;
    private double groupByP = 0.68;
    private int minLimit = 3;
    private int maxLimit = 20;
    private int groupByTransferCost = 10 * 60;
    private Instant latestDepartureTimeLimit = null;
    private boolean debug;
    private Consumer<Itinerary> maxLimitReachedSubscriber;


    /** @param arriveBy Used to set the correct sort order.  */
    public ItineraryFilterChainBuilder(boolean arriveBy) {
        this.arriveBy = arriveBy;
    }

    /**
     * Max departure time. This is a absolute filter on the itinerary departure time from the
     * origin. This do not respect the {@link #setApproximateMinLimit(int)}.
     */
    public void setLatestDepartureTimeLimit(Instant latestDepartureTimeLimit) {
        this.latestDepartureTimeLimit = latestDepartureTimeLimit;
    }

    /**
     * Set a guideline for the minimum number of itineraries to return. Some filters may respect a
     * minimum number of elements to keep when filtering and stop reducing the number when this
     * limit is reached. This depend on the filter and the intended use case.
     * <p>
     * For example the group-by filter will keep 2 samples in each group if there is 2 groups and
     * the min-limit is 3 ~ keeping up to 4 itineraries (approximately 3).
     */
    public void setApproximateMinLimit(int minLimit) {
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
     * If the maximum number of itineraries is exceeded, then the excess itineraries are removed.
     * To get notified about this a subscriber can be added. The first itinerary removed by the
     * {@code maxLimit} is retuned. The 'maxLimit' check is last thing happening in the
     * filter-chain after the final sort. So, if another filter remove an itinerary, the
     * itinerary is not considered with the respect to this feature.
     *
     * @param maxLimitReachedSubscriber the subscriber to notify in case any elements are removed.
     *                                  Only the first element removed is passed to the subscriber.
     */
    public void setMaxLimitReachedSubscriber(Consumer<Itinerary> maxLimitReachedSubscriber) {
        this.maxLimitReachedSubscriber = maxLimitReachedSubscriber;
    }

    /**
     * This will NOT delete itineraries, but tag them as deleted using the
     * {@link Itinerary#systemNotices}.
     */
    public void debug() {
        this.debug = true;
    }

    public ItineraryFilter build() {
        List<ItineraryFilter> filters = new ArrayList<>();

        filters.add(new LongTransitWalkingFilter());

        filters.add(new GroupByFilter<>(
                "groupBy-legs-filter-on-cost",
                it -> new GroupByLongestLegsId(it, groupByP),
                new SortOnGeneralizedCost(groupByTransferCost),
                minLimit
        ));

        if (latestDepartureTimeLimit != null) {
            filters.add(new LatestDepartureTimeFilter(latestDepartureTimeLimit));
        }

        // Sort itineraries
        filters.add(new OtpDefaultSortOrder(arriveBy));

        // Remove itineraries if max limit is exceeded
        if (maxLimit >= minLimit) {
            filters.add(new MaxLimitFilter("MAX", maxLimit, maxLimitReachedSubscriber));
        }

        if(debug) {
            filters = addDebugWrappers(filters);
        }

        return new FilterChain(filters);
    }


    /* private methods */

    private List<ItineraryFilter> addDebugWrappers(List<ItineraryFilter> filters) {
        final DebugFilterWrapper.Factory factory = new DebugFilterWrapper.Factory();
        return filters.stream().map(factory::wrap).collect(Collectors.toList());
    }
}
