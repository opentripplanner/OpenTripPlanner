package org.opentripplanner.updater;

import java.util.function.Supplier;
import org.opentripplanner.service.transitalert.TransitAlertRepository;
import org.opentripplanner.transit.service.TransitService;
import org.opentripplanner.updater.trip.gtfs.GtfsRealtimeFuzzyTripMatcher;

public class DefaultAlertRealTimeUpdateContext implements AlertRealTimeUpdateContext {

  /**
   * Resolved lazily so that tasks that never touch the alerts do not cause a needless alert
   * snapshot to be created and published at commit.
   */
  private final Supplier<TransitAlertRepository> transitAlertRepository;

  private final TransitService transitService;

  public DefaultAlertRealTimeUpdateContext(
    Supplier<TransitAlertRepository> transitAlertRepository,
    TransitService transitService
  ) {
    this.transitAlertRepository = transitAlertRepository;
    this.transitService = transitService;
  }

  @Override
  public TransitAlertRepository transitAlertRepository() {
    return transitAlertRepository.get();
  }

  @Override
  public TransitService transitService() {
    return transitService;
  }

  @Override
  public GtfsRealtimeFuzzyTripMatcher gtfsRealtimeFuzzyTripMatcher() {
    return new GtfsRealtimeFuzzyTripMatcher(transitService);
  }
}
