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
     * This optimization use heuristics to make a list of possible intermediate stops and prune
     * the multi-criteria search. The {@link SearchParams#numberOfAdditionalTransfers()}
     * and the heuristics is used calculate the <em>maximum number of transfers - MNT</em>. The
     * heuristics also give us the number of stops to destination at an intermediate stop. So,
     * we can use this sto terminate the search at any stop witch exceeds the limit.
     * <p/>
     * This only apply to: multi-criteria search.
     *
     * @deprecated This optimization is less efficient than the
     * {@link #PARETO_CHECK_AGAINST_DESTINATION}. We might remove this in the future, but we keep it
     * for now, because there are potential use-cases for it, like filtering on region.
     */
    @Deprecated
    TRANSFERS_STOP_FILTER,

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
