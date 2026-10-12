package org.opentripplanner.updater;

import java.util.function.Supplier;
import org.opentripplanner.service.realtimevehicles.RealtimeVehicleRepository;
import org.opentripplanner.transit.repository.TimetableRepository;
import org.opentripplanner.transit.service.TransitRepository;
import org.opentripplanner.updater.trip.gtfs.GtfsRealtimeFuzzyTripMatcher;
import org.opentripplanner.updater.trip.siri.EntityResolver;

public class DefaultTransitRealTimeUpdateContext implements TransitRealTimeUpdateContext {

  private final TransitRepository transitRepository;
  private final TimetableRepository timetableRepository;

  /**
   * Resolved lazily so that tasks that never touch the realtime vehicles do not cause a needless
   * vehicle snapshot to be created and published at commit.
   */
  private final Supplier<RealtimeVehicleRepository> realtimeVehicleRepository;

  public DefaultTransitRealTimeUpdateContext(
    TransitRepository transitRepository,
    TimetableRepository timetableRepository,
    Supplier<RealtimeVehicleRepository> realtimeVehicleRepository
  ) {
    this.transitRepository = transitRepository;
    this.timetableRepository = timetableRepository;
    this.realtimeVehicleRepository = realtimeVehicleRepository;
  }

  @Override
  public TimetableRepository timetableRepository() {
    return timetableRepository;
  }

  @Override
  public TransitRepository transitRepository() {
    return transitRepository;
  }

  @Override
  public RealtimeVehicleRepository realtimeVehicleRepository() {
    return realtimeVehicleRepository.get();
  }

  @Override
  public GtfsRealtimeFuzzyTripMatcher gtfsRealtimeFuzzyTripMatcher() {
    return new GtfsRealtimeFuzzyTripMatcher(timetableRepository);
  }

  @Override
  public EntityResolver entityResolver(String feedId) {
    return new EntityResolver(timetableRepository, transitRepository, feedId);
  }
}
