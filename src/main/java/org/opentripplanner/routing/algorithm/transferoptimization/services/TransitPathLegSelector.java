package org.opentripplanner.routing.algorithm.transferoptimization.services;

import java.util.HashSet;
import java.util.Set;
import org.opentripplanner.routing.algorithm.transferoptimization.model.MinCostFilterChain;
import org.opentripplanner.routing.algorithm.transferoptimization.model.OptimizedPathTail;
import org.opentripplanner.transit.raptor.api.transit.RaptorTripSchedule;
import org.opentripplanner.util.time.TimeUtils;


/**
 * This class takes a list of transit legs and returns the best leg based on the
 * {@link TransferOptimizedFilterFactory} and the min-time-limit. The leg arrival-time must be
 * AFTER the min-time-limit. The filter is used to pick the best leg from the legs satisfying the
 * min-time-limit.
 *
 * @param <T> The TripSchedule type defined by the user of the raptor API.
 */
class TransitPathLegSelector<T extends RaptorTripSchedule> {

    private final MinCostFilterChain<OptimizedPathTail<T>> filter;
    private Set<OptimizedPathTail<T>> remindingLegs;
    private Set<OptimizedPathTail<T>> currentTails;

    private int lastLimit = Integer.MAX_VALUE;


    TransitPathLegSelector(
            final MinCostFilterChain<OptimizedPathTail<T>> filter,
            final Set<OptimizedPathTail<T>> legs
    ) {
        this.filter = filter;
        this.remindingLegs = Set.copyOf(legs);
        this.currentTails = new HashSet<>();
    }

    Set<OptimizedPathTail<T>> next(final int minTimeLimit) {
        if(minTimeLimit > lastLimit) {
            throw new IllegalStateException(
                    "The next method must be called with decreasing time limits. "
                            + "minTimeLimit=" + TimeUtils.timeToStrLong(minTimeLimit)
                            + ", lastLimit=" + TimeUtils.timeToStrLong(lastLimit)
            );
        }
        lastLimit = minTimeLimit;

        Set<OptimizedPathTail<T>> candidates = new HashSet<>();
        Set<OptimizedPathTail<T>> rest = new HashSet<>();

        for (OptimizedPathTail<T> it : remindingLegs) {
            if( minTimeLimit < it.getLeg().toTime()) {
                candidates.add(it);
            }
            else {
                rest.add(it);
            }
        }

        if(candidates.isEmpty()) { return currentTails; }

        candidates.addAll(currentTails);

        // Set state
        remindingLegs = rest;
        currentTails = filter.filter(candidates);

        return currentTails;
    }
}
