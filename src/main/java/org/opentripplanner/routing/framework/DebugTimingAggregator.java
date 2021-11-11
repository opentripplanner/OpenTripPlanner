package org.opentripplanner.routing.framework;

import io.micrometer.core.instrument.Clock;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.Timer;
import java.util.ArrayList;
import java.util.List;
import org.opentripplanner.api.resource.DebugOutput;
import org.opentripplanner.api.resource.TransitTimingOutput;
import org.opentripplanner.ext.actuator.ActuatorAPI;
import org.opentripplanner.util.OTPFeature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Keeps account of timing information within the different parts of the routing process, and is
 * responsible for logging that information.
 */
public class DebugTimingAggregator {

  private static final Logger LOG = LoggerFactory.getLogger(DebugTimingAggregator.class);

  private static final long nanosToMillis = 1000000;

  public static final Clock clock = OTPFeature.ActuatorAPI.isOn()
          ? ActuatorAPI.prometheusRegistry.config().clock()
          : Clock.SYSTEM;

  private static final MeterRegistry registry = OTPFeature.ActuatorAPI.isOn()
          ? ActuatorAPI.prometheusRegistry
          : Metrics.globalRegistry;

  private static final Timer directStreetRouterTimer = Timer.builder("routing.directStreet").register(registry);
  private static final Timer directFlexRouterTimer =  Timer.builder("routing.directFlex").register(registry);

  private static final Timer accessTimer = Timer.builder("routing.access").register(registry);
  private static final Timer egressTimer = Timer.builder("routing.egress").register(registry);
  private static final DistributionSummary numAccessesDistribution = DistributionSummary
          .builder("routing.numAccess")
          .register(registry);
  private static final DistributionSummary numEgressesDistribution = DistributionSummary
          .builder("routing.numEgress")
          .register(registry);

  private static final Timer preCalculationTimer = Timer.builder("routing.preCalculation").register(registry);
  private static final Timer tripPatternFilterTimer = Timer.builder("routing.tripPatternFiltering").register(registry);
  private static final Timer accessEgressTimer = Timer.builder("routing.accessEgress").register(registry);
  private static final Timer raptorSearchTimer = Timer.builder("routing.raptor").register(registry);
  private static final Timer itineraryCreationTimer = Timer.builder("routing.itineraryCreation").register(registry);
  private static final Timer transitRouterTimer = Timer.builder("routing.transit").register(registry);
  private static final Timer filteringTimer = Timer.builder("routing.filtering").register(registry);
  private static final Timer renderingTimer = Timer.builder("routing.rendering").register(registry);
  private static final Timer routingTotalTimer = Timer.builder("routing.router").register(registry);
  private static final Timer requestTotalTimer = Timer.builder("routing.total").register(registry);

  private final Timer.Sample startedCalculating;

  private Timer.Sample startedDirectStreetRouter;
  private long directStreetRouterTime;

  private Timer.Sample startedDirectFlexRouter;
  private long directFlexRouterTime;

  private Timer.Sample finishedPatternFiltering;
  private Timer.Sample finishedAccessEgress;
  private Timer.Sample finishedRaptorSearch;

  private Timer.Sample finishedRouters;
  private Timer.Sample finishedFiltering;

  private Timer.Sample startedAccessCalculating;
  private Timer.Sample startedEgressCalculating;
  private long accessTime;
  private long egressTime;
  private int numAccesses;
  private int numEgresses;

  private long precalculationTime;
  private Timer.Sample startedTransitRouterTime;
  private long tripPatternFilterTime;
  private long accessEgressTime;
  private long raptorSearchTime;
  private long itineraryCreationTime;
  private long transitRouterTime;
  private long filteringTime;
  private long renderingTime;
  private long requestTotalTime;
  private final List<String> messages = new ArrayList<>();

  /**
   * Record the time when we first began calculating a path for this request. Note that timings will not
   * include network and server request queue overhead, which is what we want.
   */
  public DebugTimingAggregator() {
    startedCalculating = Timer.start(clock);
  }

  /**
   * Record the time when the worker initialization is done, and the direct street router starts.
   */
  public void finishedPrecalculating() {
    precalculationTime = startedCalculating.stop(preCalculationTimer) / nanosToMillis;
    log("┌  Routing initialization", precalculationTime);
  }

  /** Record the time when starting the direct street router search. */
  public void startedDirectStreetRouter() {
    startedDirectStreetRouter = Timer.start(clock);
  }

  /** Record the time when we finished the direct street router search. */
  public void finishedDirectStreetRouter() {
    directStreetRouterTime = startedDirectStreetRouter.stop(directStreetRouterTimer) / nanosToMillis;
  }

  /** Record the time when starting the direct flex router search. */
  public void startedDirectFlexRouter() {
    startedDirectFlexRouter = Timer.start(clock);
  }

  /** Record the time when we finished the direct flex router search. */
  public void finishedDirectFlexRouter() {
    directFlexRouterTime = startedDirectFlexRouter.stop(directFlexRouterTimer) / nanosToMillis;
  }

  /** Record the time when starting the transit router search. */
  public void startedTransitRouting() {
    startedTransitRouterTime = Timer.start(clock);
  }

  /**
   * Record the time when we are finished with the creation of the raptor data models.
   */
  public void finishedPatternFiltering() {
    finishedPatternFiltering = Timer.start(clock);
    tripPatternFilterTime = startedTransitRouterTime.stop(tripPatternFilterTimer) / nanosToMillis;
  }

  public void startedAccessCalculating() {
    startedAccessCalculating = Timer.start(clock);
  }

  public void finishedAccessCalculating() {
    accessTime = startedAccessCalculating.stop(accessTimer) / nanosToMillis;
  }

  public void startedEgressCalculating() {
    startedEgressCalculating = Timer.start(clock);
  }

  public void finishedEgressCalculating() {
    egressTime = startedEgressCalculating.stop(egressTimer) / nanosToMillis;
  }

  /**
   * Record the time when we are finished with the access and egress routing.
   */
  public void finishedAccessEgress(int numAccesses, int numEgresses) {
    finishedAccessEgress = Timer.start(clock);
    accessEgressTime = finishedPatternFiltering.stop(accessEgressTimer) / nanosToMillis;
    this.numAccesses = numAccesses;
    numAccessesDistribution.record(numAccesses);
    this.numEgresses = numEgresses;
    numEgressesDistribution.record(numEgresses);
  }

  /**
   * Record the time when we are finished with the raptor search.
   */
  public void finishedRaptorSearch() {
    finishedRaptorSearch = Timer.start(clock);
    raptorSearchTime = finishedAccessEgress.stop(raptorSearchTimer) / nanosToMillis;
  }

  /**
   * Record the time when we have created internal itinerary objects from the raptor responses.
   */
  public void finishedItineraryCreation() {
    itineraryCreationTime = finishedRaptorSearch.stop(itineraryCreationTimer) / nanosToMillis;
  }

  /** Record the time when we finished the transit router search */
  public void finishedTransitRouter() {
    transitRouterTime = startedTransitRouterTime.stop(transitRouterTimer) / nanosToMillis;
  }

  public void finishedRouting() {
    long routingTotalTime = startedCalculating.stop(routingTotalTimer) / nanosToMillis;

    finishedRouters = Timer.start(clock);
    if (directStreetRouterTime > 0) {
      log("├  Direct street routing", directStreetRouterTime);
    }
    if (directFlexRouterTime > 0) {
      log("├  Direct flex routing", directFlexRouterTime);
    }

    if (transitRouterTime > 0) {
      log("│┌ Creating raptor data model", tripPatternFilterTime);
      log("│├ Access routing (" + numAccesses + " accesses)", accessTime);
      log("│├ Egress routing ("+ numEgresses +" egresses)", egressTime);
      log("││ Access/Egress routing", accessEgressTime);
      log("│├ Main routing", raptorSearchTime);
      log("│├ Creating itineraries", itineraryCreationTime);
      log("├┴ Transit routing total", transitRouterTime);
    }

    log("│  Routing total: ", routingTotalTime);
  }
  /** Record the time when we finished filtering the paths for this request. */
  public void finishedFiltering() {
    finishedFiltering = Timer.start(clock);
    filteringTime = finishedRouters.stop(filteringTimer) / nanosToMillis;
    log("├  Filtering itineraries", filteringTime);
  }

  /** Record the time when we finished converting the internal model to API classes */
  @SuppressWarnings("Convert2MethodRef")
  public DebugOutput finishedRendering() {
    renderingTime =  finishedFiltering.stop(renderingTimer) / nanosToMillis;
    requestTotalTime = startedCalculating.stop(requestTotalTimer) / nanosToMillis;
    log("├  Converting model objects", renderingTime);
    log("┴  Request total", requestTotalTime);
    messages.forEach(m -> LOG.debug(m));
    return getDebugOutput();
  }

  /** Summarize and calculate elapsed times. */
  private DebugOutput getDebugOutput() {

    return new DebugOutput(
        precalculationTime,
        directStreetRouterTime,
        transitRouterTime,
        filteringTime,
        renderingTime,
        requestTotalTime,
        new TransitTimingOutput(
            tripPatternFilterTime,
            accessEgressTime,
            raptorSearchTime,
            itineraryCreationTime
        )
    );
  }

  private void log(String msg, long millis) {
    messages.add(String.format("%-36s: %5s ms", msg, millis));
  }
}
