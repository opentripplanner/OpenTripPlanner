package org.opentripplanner.updater;

import java.util.function.Supplier;
import org.opentripplanner.service.realtimevehicles.RealtimeVehicleRepository;
import org.opentripplanner.transit.repository.TimetableRepository;
import org.opentripplanner.transit.service.DefaultTransitService;
import org.opentripplanner.transit.service.TransitRepository;
import org.opentripplanner.transit.service.TransitService;
import org.opentripplanner.updater.trip.gtfs.GtfsRealtimeFuzzyTripMatcher;
import org.opentripplanner.updater.trip.siri.EntityResolver;

public class DefaultTransitRealTimeUpdateContext implements TransitRealTimeUpdateContext {

  private final TimetableRepository timetableRepository;
  private final TransitService transitService;

  /**
   * Resolved lazily so that tasks that never touch the realtime vehicles do not cause a needless
   * vehicle snapshot to be created and published at commit.
   */
  private final Supplier<RealtimeVehicleRepository> realtimeVehicleRepository;

  /**
   * The context needs the mutable repository so that entity lookups (trips, routes, patterns) see
   * all in-progress real-time additions that have not yet been committed to a published snapshot.
   * <p>
   * A {@link TimetableRepository} cannot be used directly for these lookups, because every
   * lookup must also fall back to scheduled data in the {@link TransitRepository} when an entity
   * is not found in the real-time repository. The {@link DefaultTransitService} combines both: it
   * checks the repository first, then falls back to the static index.
   * <p>
   * {@link DefaultTransitService} accepts a {@link org.opentripplanner.transit.repository.TimetableRepositorySnapshot},
   * because in request scope it must never receive the mutable repository. Passing the repository
   * here is safe because {@link TimetableRepository} extends
   * {@link org.opentripplanner.transit.repository.TimetableRepositorySnapshot}. A cleaner separation
   * would require merging scheduled and real-time data into a single unified store - this is the end goal!
   */
  public DefaultTransitRealTimeUpdateContext(
    TransitRepository transitRepository,
    TimetableRepository timetableRepository,
    Supplier<RealtimeVehicleRepository> realtimeVehicleRepository
  ) {
    this.timetableRepository = timetableRepository;
    this.transitService = new DefaultTransitService(transitRepository, timetableRepository);
    this.realtimeVehicleRepository = realtimeVehicleRepository;
  }

  /**
   * Constructor for unit tests only.
   */
  public DefaultTransitRealTimeUpdateContext(TransitRepository transitRepository) {
    this(transitRepository, null, () -> {
      throw new UnsupportedOperationException(
        "The realtime-vehicle repository is not available in this test context"
      );
    });
  }

  @Override
  public TimetableRepository timetableRepository() {
    return timetableRepository;
  }

  @Override
  public RealtimeVehicleRepository realtimeVehicleRepository() {
    return realtimeVehicleRepository.get();
  }

  @Override
  public TransitService transitService() {
    return transitService;
  }

  @Override
  public GtfsRealtimeFuzzyTripMatcher gtfsRealtimeFuzzyTripMatcher() {
    return new GtfsRealtimeFuzzyTripMatcher(transitService);
  }

  @Override
  public EntityResolver entityResolver(String feedId) {
    return new EntityResolver(transitService, feedId);
  }
}
