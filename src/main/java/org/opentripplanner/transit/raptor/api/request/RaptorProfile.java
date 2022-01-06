package org.opentripplanner.transit.raptor.api.request;


import java.util.stream.Stream;

/**
 * Several implementation are implemented - with different behaviour. Use the one
 * that suites your need best.
 */
public enum RaptorProfile {
    /**
     * Multi criteria pareto search.
     */
    MULTI_CRITERIA("Mc"),
    /**
     * Range Raptor finding the earliest arrival time, shortest travel duration and fewest transfers.
     * The cost is not used.
     * <p/>
     * Computes result paths.
     */
    STANDARD("Standard"),

    /**
     * Range Raptor finding the earliest arrival times - only. No paths are returned.
     * Only one iteration is performed. This search is used internally to calculate
     * heuristics.
     * <p/>
     * Computes best/min travel duration and number of transfers.
     */
    BEST_TIME("StdBestTime"),

    /**
     * Same as {@link #STANDARD}, but skip extra wait time - only board slack.
     * <p/>
     * Computes best/min travel duration and number of transfers.
     */
    NO_WAIT_STD("NoWaitStd"),

    /**
     * Same as {@link #BEST_TIME}, but skip extra wait time - only board slack.
     * <p/>
     * Computes best/min travel duration(without wait-time) and number of transfers.
     */
    NO_WAIT_BEST_TIME("NoWaitBT");


    private final String abbreviation;

    RaptorProfile(String abbreviation) {
        this.abbreviation = abbreviation;
    }

    public final String abbreviation() {
        return abbreviation;
    }

    public boolean is(RaptorProfile candidate) {
        return this == candidate;
    }

    public boolean isOneOf(RaptorProfile... candidates) {
        return Stream.of(candidates).anyMatch(this::is);
    }

    /**
     * The {@link org.opentripplanner.transit.raptor.rangeraptor.standard.NoWaitTransitWorker}
     * will time-shift the arrival-times, so we need to use the approximate trip-times search in
     * path construction. The BEST_TIME state should not have path construction, but we include it
     * here anyway.
     */
    public boolean useApproximateTripSearch() {
        return isOneOf(NO_WAIT_STD, NO_WAIT_BEST_TIME);
    }
}
