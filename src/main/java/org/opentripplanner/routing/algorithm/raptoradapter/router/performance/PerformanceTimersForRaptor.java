package org.opentripplanner.routing.algorithm.raptoradapter.router.performance;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.util.Collection;
import org.opentripplanner.routing.api.request.RoutingTag;
import org.opentripplanner.routing.framework.MicrometerUtils;
import org.opentripplanner.transit.raptor.api.debug.RaptorTimers;

public class PerformanceTimersForRaptor implements RaptorTimers {

  // Variables to track time spent
  private final Timer timerRoute;
  private final Timer findTransitPerRound;
  private final Timer findTransfersPerRound;

  public PerformanceTimersForRaptor(
    String namePrefix,
    Collection<RoutingTag> routingTags,
    MeterRegistry registry
  ) {
    var tags = MicrometerUtils.mapTimingTags(routingTags);
    timerRoute = Timer.builder("raptor." + namePrefix + ".route").tags(tags).register(registry);
    findTransitPerRound =
      Timer.builder("raptor." + namePrefix + ".minute.transit").tags(tags).register(registry);
    findTransfersPerRound =
      Timer.builder("raptor." + namePrefix + ".minute.transfers").tags(tags).register(registry);
  }

  public Timer timerRoute() {
    return timerRoute;
  }

  public Timer timerFindTransitPerRound() {
    return findTransitPerRound;
  }

  public Timer timerFindTransfersPerRound() {
    return findTransfersPerRound;
  }

  @Override
  public void route(Runnable body) {
    timerRoute.record(body);
  }

  @Override
  public void findTransitForRound(Runnable body) {
    findTransitPerRound.record(body);
  }

  @Override
  public void findTransfersForRound(Runnable body) {
    findTransfersPerRound.record(body);
  }
}
