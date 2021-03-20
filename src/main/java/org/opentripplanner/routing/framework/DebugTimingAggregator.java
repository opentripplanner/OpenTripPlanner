package org.opentripplanner.routing.framework;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import org.opentripplanner.api.resource.DebugOutput;
import org.opentripplanner.api.resource.TransitTimingOutput;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Keeps account of timing information within the different parts of the routing process, and is
 * responsible of logging that information.
 */
public class DebugTimingAggregator {
  private static final Logger LOG = LoggerFactory.getLogger(DebugTimingAggregator.class);

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

  private final boolean notEnabled = !LOG.isDebugEnabled();

  private final List<String> messages = new ArrayList<>();

  /**
   * Record the time when we first began calculating a path for this request. Note that timings will not
   * include network and server request queue overhead, which is what we want.
   */
  public void startedCalculating() {
    if(notEnabled) { return; }
    startedCalculating = System.currentTimeMillis();
  }

  /**
   * Record the time when the worker initialization is done, and the direct street router starts.
   */
  public void finishedPrecalculating() {
    if(notEnabled) { return; }
    finishedPrecalculating = System.currentTimeMillis();
    precalculationTime = finishedPrecalculating - startedCalculating;
    log("┌  Routing initialization", directStreetRouterTime);
  }

  /** Record the time when we finished the direct street router search. */
  public void finishedDirectStreetRouter() {
    if(notEnabled) { return; }
    finishedDirectStreetRouter = System.currentTimeMillis();
    directStreetRouterTime = finishedDirectStreetRouter - finishedPrecalculating;
    log("├  Direct street routing", directStreetRouterTime);
  }

  /**
   * Record the time when we are finished with the creation of the raptor data models.
   */
  public void finishedPatternFiltering() {
    if(notEnabled) { return; }
    finishedPatternFiltering = System.currentTimeMillis();
    tripPatternFilterTime = finishedPatternFiltering - finishedDirectStreetRouter;
    log("│┌ Filtering tripPatterns", tripPatternFilterTime);
  }

  /**
   * Record the time when we are finished with the access and egress routing.
   */
  public void finishedAccessEgress() {
    if(notEnabled) { return; }
    finishedAccessEgress = System.currentTimeMillis();
    accessEgressTime = finishedAccessEgress - finishedPatternFiltering;
    log("│├ Access/egress routing", accessEgressTime);
  }

  /**
   * Record the time when we are finished with the raptor search.
   */
  public void finishedRaptorSearch() {
    if(notEnabled) { return; }
    finishedRaptorSearch = System.currentTimeMillis();
    raptorSearchTime = finishedRaptorSearch - finishedAccessEgress;
    log("│├ Main routing", raptorSearchTime);
  }

  /**
   * Record the time when we have created internal itinerary objects from the raptor responses.
   */
  public void finishedItineraryCreation() {
    if(notEnabled) { return; }
    itineraryCreationTime = System.currentTimeMillis() - finishedRaptorSearch;
    log("│├ Creating itineraries", itineraryCreationTime);
  }

  /** Record the time when we finished the tranist router search */
  public void finishedTransitRouter() {
    if(notEnabled) { return; }
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
    if(notEnabled) { return; }
    finishedFiltering = System.currentTimeMillis();
    filteringTime = finishedFiltering - finishedTransitRouter;
    log("├  Filtering itineraries", filteringTime);
  }

  /** Record the time when we finished converting the internal model to API classes */
  @SuppressWarnings("Convert2MethodRef")
  @Nullable
  public DebugOutput finishedRendering() {
    if(notEnabled) { return null; }
    finishedRendering = System.currentTimeMillis();
    renderingTime = finishedRendering - finishedFiltering;
    log("├  Converting model objects", renderingTime);
    log("┴  Request total", finishedRendering - startedCalculating);
    messages.forEach(m -> LOG.debug(m));
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

  private void log(String msg, long millis) {
    messages.add(String.format("%-30s: %5s ms", msg, millis));
  }
}
