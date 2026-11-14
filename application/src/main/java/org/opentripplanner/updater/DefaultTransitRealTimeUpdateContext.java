package org.opentripplanner.updater;

import java.util.function.Supplier;
import org.opentripplanner.service.realtimevehicles.RealtimeVehicleRepository;
import org.opentripplanner.transit.repository.TimetableRepository;
import org.opentripplanner.transit.repository.TimetableRepositorySnapshot;
import org.opentripplanner.transit.service.TransitRepository;
import org.opentripplanner.updater.trip.gtfs.GtfsRealtimeFuzzyTripMatcher;
import org.opentripplanner.updater.trip.siri.EntityResolver;

public class DefaultTransitRealTimeUpdateContext implements TransitRealTimeUpdateContext {

  private final TransitRepository transitRepository;

  /**
   * Resolved lazily so that tasks that never write to the realtime timetable, such as
   * vehicle-position updates, do not cause a needless timetable snapshot to be created and
   * published at commit.
   */
  private final Supplier<TimetableRepository> timetableRepository;

  /**
   * Resolved lazily because only tasks that must not see uncommitted timetable changes, such as
   * vehicle-position updates, need the last committed snapshot.
   */
  private final Supplier<TimetableRepositorySnapshot> committedTimetableSnapshot;

  /**
   * Resolved lazily so that tasks that never touch the realtime vehicles do not cause a needless
   * vehicle snapshot to be created and published at commit.
   */
  private final Supplier<RealtimeVehicleRepository> realtimeVehicleRepository;

  public DefaultTransitRealTimeUpdateContext(
    TransitRepository transitRepository,
    Supplier<TimetableRepository> timetableRepository,
    Supplier<TimetableRepositorySnapshot> committedTimetableSnapshot,
    Supplier<RealtimeVehicleRepository> realtimeVehicleRepository
  ) {
    this.transitRepository = transitRepository;
    this.timetableRepository = timetableRepository;
    this.committedTimetableSnapshot = committedTimetableSnapshot;
    this.realtimeVehicleRepository = realtimeVehicleRepository;
  }

  @Override
  public TimetableRepository timetableRepository() {
    return timetableRepository.get();
  }

  @Override
  public TimetableRepositorySnapshot committedTimetableSnapshot() {
    return committedTimetableSnapshot.get();
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
    return new GtfsRealtimeFuzzyTripMatcher(timetableRepository());
  }

  @Override
  public EntityResolver entityResolver(String feedId) {
    return new EntityResolver(timetableRepository(), transitRepository, feedId);
  }
}
