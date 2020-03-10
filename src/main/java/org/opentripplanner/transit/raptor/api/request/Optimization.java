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
     * <p/>
     * This only apply to: multi-criteria search.
     */
    PARALLEL,

    /**
     * This optimization use heuristics at each stop calculate an optimistic estimate for all
     * criteria at the destination. Then this "vector" is checked if it qualify in the existing
     * set of pareto optimal destination arrivals.
     * <p/>
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
