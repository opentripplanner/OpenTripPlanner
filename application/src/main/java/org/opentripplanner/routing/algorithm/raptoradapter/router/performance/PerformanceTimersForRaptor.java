package org.opentripplanner.routing.algorithm.raptoradapter.router.performance;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.util.Collection;
import org.opentripplanner.raptor.api.debug.RaptorTimers;
import org.opentripplanner.routing.api.request.RoutingTag;
import org.opentripplanner.routing.framework.MicrometerUtils;

public class PerformanceTimersForRaptor implements RaptorTimers {

  // Variables to track time spent
  private final Timer timerRoute;
  private final Timer routeTransitTimer;
  private final Timer applyTransfersTimer;
  private final MeterRegistry registry;
  private final Collection<RoutingTag> routingTags;

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
    timerRoute.record(body);
  }

  @Override
  public void routeTransit(Runnable body) {
    routeTransitTimer.record(body);
  }

  @Override
  public void applyTransfers(Runnable body) {
    applyTransfersTimer.record(body);
  }

  @Override
  public RaptorTimers withNamePrefix(String namePrefix) {
    return new PerformanceTimersForRaptor(namePrefix, routingTags, registry);
  }
}
