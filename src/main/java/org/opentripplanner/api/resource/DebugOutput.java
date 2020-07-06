package org.opentripplanner.api.resource;

/**
 * Holds information to be included in the REST Response for debugging and profiling purposes.
 */
public class DebugOutput {
    public final long precalculationTime;
    public final long directStreetRouterTime;
    public final long transitRouterTime;
    public final long filteringTime;
    public final long renderingTime;
    public final long totalTime;

    public final TransitTimingOutput transitRouterTimes;

    public DebugOutput(
        long precalculationTime, long directStreetRouterTime, long transitRouterTime,
        long filteringTime, long renderingTime, long totalTime,
        TransitTimingOutput transitRouterTimes
    ) {
        this.precalculationTime = precalculationTime;
        this.directStreetRouterTime = directStreetRouterTime;
        this.transitRouterTime = transitRouterTime;
        this.filteringTime = filteringTime;
        this.renderingTime = renderingTime;
        this.totalTime = totalTime;
        this.transitRouterTimes = transitRouterTimes;
    }

}
