package org.opentripplanner.api.resource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Holds information to be included in the REST Response for debugging and profiling purposes.
 */
public class DebugOutput {

    private static final Logger LOG = LoggerFactory.getLogger(DebugOutput.class);

    /* Only public fields are serialized by JAX-RS, make interal ones private? */
    private long startedCalculating;
    private long finishedDirectStreetRouter;
    private long finishedTransitRouter;
    private long finishedFiltering;
    private long finishedRendering;

    /* Results, public to cause JAX-RS serialization */
    public long directStreetRouterTime;
    public long transitRouterTime;
    public long filteringTime;
    public long renderingTime;
    public long totalTime;

    /**
     * Record the time when we first began calculating a path for this request. Note that timings will not
     * include network and server request queue overhead, which is what we want.
     */
    public void startedCalculating() {
        startedCalculating = System.currentTimeMillis();
    }

    /** Record the time when we finished the direct street router search. */
    public void finishedDirectStreetRouter() {
        finishedDirectStreetRouter = System.currentTimeMillis();
    }

    /** Record the time when we finished the tranist router search */
    public void foundPath() {
        finishedTransitRouter = System.currentTimeMillis();
    }

    /** Record the time when we finished filtering the paths for this request. */
    public void finishedFiltering() {
        finishedFiltering = System.currentTimeMillis();
    }

    /** Record the time when we finished converting the internal model to API classes */
    public void finishedRendering() {
        finishedRendering = System.currentTimeMillis();
        computeSummary();
    }

    /** Summarize and calculate elapsed times. */
    private void computeSummary() {
        directStreetRouterTime = finishedDirectStreetRouter - startedCalculating;
        transitRouterTime = finishedTransitRouter - finishedDirectStreetRouter;
        filteringTime = finishedFiltering - finishedTransitRouter;
        renderingTime = finishedRendering - finishedFiltering;
        totalTime = finishedRendering - startedCalculating;
    }
}
