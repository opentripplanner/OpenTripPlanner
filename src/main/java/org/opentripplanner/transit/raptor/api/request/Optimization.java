package org.opentripplanner.transit.raptor.api.request;


import java.util.Collection;

/**
 * Here is a list of all optimizations that is implemented. All optimization can be combined
 * in one search.
 */
public enum Optimization {

    /**
     * Run part of the search in parallel. This uses more resources and may degrade the overall
     * performance, but each individual travel search should be faster.
     * <p>
     * This only apply to: multi-criteria search.
     */
    PARALLEL,

    /**
     * This optimization use heuristics at each stop calculate an optimistic estimate for all
     * criteria at the destination. Then this "vector" is checked if it qualify in the existing
     * set of pareto optimal destination arrivals.
     * <p>
     * On the Norwegian graph this improve the average search time from 1.15 seconds to 0.55 seconds
     * for a test set of 27 different test cases with an average search-window of 1 hour and
     * 20 minutes. The longer searches have search-windows up to 3 hours. The SpeedTest is used to
     * obtain the results.
     * <p>
     * This optimization was not implemented based on the [Restricted Pareto Sets]
     * (https://epubs.siam.org/doi/pdf/10.1137/1.9781611975499.5), but it is very similar. The
     * current implementation do not use a separate Raptor search for the cost-criteria, but
     * guess the cost based on the minimum-travel-time-to-destination and
     * minimum-number-of-transfers-to-destination.
     * <p>
     * This only apply to: multi-criteria search.
     */
    PARETO_CHECK_AGAINST_DESTINATION;


    public boolean is(Optimization other) {
        return this == other;
    }

    /**
     * Is this in the given collection?
     */
    public boolean isOneOf(Collection<Optimization> others) {
        return others.stream().anyMatch(this::is);
    }
}
