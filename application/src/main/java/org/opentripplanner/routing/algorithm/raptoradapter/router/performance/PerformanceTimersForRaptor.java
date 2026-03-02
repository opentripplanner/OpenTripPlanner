package org.opentripplanner.routing.algorithm.raptoradapter.router.performance;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.util.Collection;
import java.util.concurrent.TimeUnit;
import org.opentripplanner.raptor.api.debug.RaptorTimers;
import org.opentripplanner.routing.api.request.RoutingTag;
import org.opentripplanner.routing.framework.MicrometerUtils;

public class PerformanceTimersForRaptor implements RaptorTimers {

  private final Timer timerRoute;
  private final Timer routeTransitTimer;
  private final Timer applyTransfersTimer;
  private final MeterRegistry registry;
  private final Collection<RoutingTag> routingTags;

  /**
   * Accumulators for sub-timings within a single route() call. Instead of calling
   * Timer.record(Runnable) on every round (~480 calls per search), we accumulate nanos
   * and report once at the end of route(). This is safe because Raptor search is
   * single-threaded and each PerformanceTimersForRaptor instance is per-request.
   */
  private long routeTransitNanos;
  private long applyTransfersNanos;

  public PerformanceTimersForRaptor(
    String namePrefix,
    Collection<RoutingTag> routingTags,
    MeterRegistry registry
  ) {
    this.registry = registry;
    this.routingTags = routingTags;
    var tags = MicrometerUtils.mapTimingTags(routingTags);
    timerRoute = Timer.builder("raptor." + namePrefix + ".route").tags(tags).register(registry);
    routeTransitTimer = Timer.builder("raptor." + namePrefix + ".minute.transit")
      .tags(tags)
      .register(registry);
    applyTransfersTimer = Timer.builder("raptor." + namePrefix + ".minute.transfers")
      .tags(tags)
      .register(registry);
  }

  @Override
  public void route(Runnable body) {
    routeTransitNanos = 0;
    applyTransfersNanos = 0;
    timerRoute.record(body);
    routeTransitTimer.record(routeTransitNanos, TimeUnit.NANOSECONDS);
    applyTransfersTimer.record(applyTransfersNanos, TimeUnit.NANOSECONDS);
  }

  @Override
  public void routeTransit(Runnable body) {
    long start = System.nanoTime();
    body.run();
    routeTransitNanos += System.nanoTime() - start;
  }

  @Override
  public void applyTransfers(Runnable body) {
    long start = System.nanoTime();
    body.run();
    applyTransfersNanos += System.nanoTime() - start;
  }

  @Override
  public RaptorTimers withNamePrefix(String namePrefix) {
    return new PerformanceTimersForRaptor(namePrefix, routingTags, registry);
  }
}
