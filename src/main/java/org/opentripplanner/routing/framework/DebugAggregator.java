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

  private void log(String msg, long millis) {
    if (LOG.isDebugEnabled()) {
      LOG.debug(String.format("%-30s: %5s ms", msg, millis));
    }
  }

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
    log("┌  Routing initialization", directStreetRouterTime);
  }

  /** Record the time when we finished the direct street router search. */
  public void finishedDirectStreetRouter() {
    finishedDirectStreetRouter = System.currentTimeMillis();
    directStreetRouterTime = finishedDirectStreetRouter - finishedPrecalculating;
    log("├  Direct street routing", directStreetRouterTime);
  }

  /**
   * Record the time when we are finished with the creation of the raptor data models.
   */
  public void finishedPatternFiltering() {
    finishedPatternFiltering = System.currentTimeMillis();
    tripPatternFilterTime = finishedPatternFiltering - finishedDirectStreetRouter;
    log("│┌ Filtering tripPatterns", tripPatternFilterTime);
  }

  /**
   * Record the time when we are finished with the access and egress routing.
   */
  public void finishedAccessEgress() {
    finishedAccessEgress = System.currentTimeMillis();
    accessEgressTime = finishedAccessEgress - finishedPatternFiltering;
    log("│├ Access/egress routing", accessEgressTime);
  }

  /**
   * Record the time when we are finished with the raptor search.
   */
  public void finishedRaptorSearch() {
    finishedRaptorSearch = System.currentTimeMillis();
    raptorSearchTime = finishedRaptorSearch - finishedAccessEgress;
    log("│├ Main routing", raptorSearchTime);
  }

  /**
   * Record the time when we have created internal itinerary objects from the raptor responses.
   */
  public void finishedItineraryCreation() {
    itineraryCreationTime = System.currentTimeMillis() - finishedRaptorSearch;
    log("│├ Creating itineraries", itineraryCreationTime);
  }

  /** Record the time when we finished the tranist router search */
  public void finishedTransitRouter() {
    finishedTransitRouter = System.currentTimeMillis();
    transitRouterTime = finishedTransitRouter - finishedDirectStreetRouter;

    if (finishedPatternFiltering > 0) {
      log("├┴ Transit routing total", transitRouterTime);
    } else {
      log("├─ Transit routing total", transitRouterTime);
    }
  }

  /** Record the time when we finished filtering the paths for this request. */
  public void finishedFiltering() {
    finishedFiltering = System.currentTimeMillis();
    filteringTime = finishedFiltering - finishedTransitRouter;
    log("├  Filtering itineraries", filteringTime);
  }

  /** Record the time when we finished converting the internal model to API classes */
  public DebugOutput finishedRendering() {
    finishedRendering = System.currentTimeMillis();
    renderingTime = finishedRendering - finishedFiltering;
    log("├  Converting model objects", renderingTime);
    log("┴  Request total", finishedRendering - startedCalculating);
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
