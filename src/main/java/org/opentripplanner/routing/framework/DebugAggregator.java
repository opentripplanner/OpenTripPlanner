package org.opentripplanner.routing.framework;

import org.opentripplanner.api.resource.DebugOutput;
import org.opentripplanner.api.resource.TransitTimingOutput;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Keeps account of timing information within the different parts of the routing process, and is
 * responsible of logging that information.
 */
public class DebugAggregator {
  private static final Logger LOG = LoggerFactory.getLogger(DebugAggregator.class);

  private long startedCalculating;
  private long finishedPrecalculating;
  private long finishedDirectStreetRouter;

  private long finishedPatternFiltering;
  private long finishedAccessEgress;
  private long finishedRaptorSearch;

  private long finishedTransitRouter;
  private long finishedFiltering;
  private long finishedRendering;

  private long precalculationTime;
  private long directStreetRouterTime;
  private long tripPatternFilterTime;
  private long accessEgressTime;
  private long raptorSearchTime;
  private long itineraryCreationTime;
  private long transitRouterTime;
  private long filteringTime;
  private long renderingTime;

  /**
   * Record the time when we first began calculating a path for this request. Note that timings will not
   * include network and server request queue overhead, which is what we want.
   */
  public void startedCalculating() {
    startedCalculating = System.currentTimeMillis();
  }

  /**
   * Record the time when the worker initialization is done, and the direct street router starts.
   */
  public void finishedPrecalculating() {
    finishedPrecalculating = System.currentTimeMillis();
    precalculationTime = finishedPrecalculating - startedCalculating;
    LOG.debug("Routing initialization took {} ms", directStreetRouterTime);
  }

  /** Record the time when we finished the direct street router search. */
  public void finishedDirectStreetRouter() {
    finishedDirectStreetRouter = System.currentTimeMillis();
    directStreetRouterTime = finishedDirectStreetRouter - finishedPrecalculating;
    LOG.debug("Direct street routing took {} ms", directStreetRouterTime);
  }

  /**
   * Record the time when we are finished with the creation of the raptor data models.
   */
  public void finishedPatternFiltering() {
    finishedPatternFiltering = System.currentTimeMillis();
    tripPatternFilterTime = finishedPatternFiltering - finishedDirectStreetRouter;
    LOG.debug("Filtering tripPatterns took {} ms", tripPatternFilterTime);
  }

  /**
   * Record the time when we are finished with the access and egress routing.
   */
  public void finishedAccessEgress() {
    finishedAccessEgress = System.currentTimeMillis();
    accessEgressTime = finishedAccessEgress - finishedPatternFiltering;
    LOG.debug("Access/egress routing took {} ms", accessEgressTime);
  }

  /**
   * Record the time when we are finished with the raptor search.
   */
  public void finishedRaptorSearch() {
    finishedRaptorSearch = System.currentTimeMillis();
    raptorSearchTime = finishedRaptorSearch - finishedAccessEgress;
    LOG.debug("Main routing took {} ms", raptorSearchTime);
  }

  /**
   * Record the time when we have created internal itinerary objects from the raptor responses.
   */
  public void finishedItineraryCreation() {
    itineraryCreationTime = System.currentTimeMillis() - finishedRaptorSearch;
    LOG.debug("Creating itineraries took {} ms", itineraryCreationTime);
  }

  /** Record the time when we finished the tranist router search */
  public void finishedTransitRouter() {
    finishedTransitRouter = System.currentTimeMillis();
    transitRouterTime = finishedTransitRouter - finishedDirectStreetRouter;
    LOG.debug("Transit routing took total {} ms", transitRouterTime);
  }

  /** Record the time when we finished filtering the paths for this request. */
  public void finishedFiltering() {
    finishedFiltering = System.currentTimeMillis();
    filteringTime = finishedFiltering - finishedTransitRouter;
    LOG.debug("Filtering took {} ms", transitRouterTime);
  }

  /** Record the time when we finished converting the internal model to API classes */
  public DebugOutput finishedRendering() {
    finishedRendering = System.currentTimeMillis();
    renderingTime = finishedRendering - finishedFiltering;
    LOG.debug("Converting model objects took {} ms", transitRouterTime);
    return getDebugOutput();
  }

  /** Summarize and calculate elapsed times. */
  private DebugOutput getDebugOutput() {
    long totalTime = finishedRendering - startedCalculating;

    return new DebugOutput(
        precalculationTime,
        directStreetRouterTime,
        transitRouterTime,
        filteringTime,
        renderingTime,
        totalTime,
        new TransitTimingOutput(
            tripPatternFilterTime,
            accessEgressTime,
            raptorSearchTime,
            itineraryCreationTime
        )
    );
  }

}
