package org.opentripplanner.updater.trip.configure;

import dagger.Module;
import dagger.Provides;
import jakarta.inject.Singleton;
import java.time.LocalDate;
import org.opentripplanner.model.TimetableSnapshot;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.RaptorTransitData;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.mappers.RealTimeRaptorTransitDataUpdater;
import org.opentripplanner.standalone.config.ConfigModel;
import org.opentripplanner.transit.service.DefaultTransitService;
import org.opentripplanner.transit.service.TimetableRepository;
import org.opentripplanner.updater.trip.TimetableSnapshotManager;
import org.opentripplanner.updater.trip.TripPatternCache;
import org.opentripplanner.updater.trip.TripPatternIdGenerator;

@Module
public abstract class TripUpdateModule {

  @Provides
  @Singleton
  public static TimetableSnapshotManager timetableSnapshotManager(
    RealTimeRaptorTransitDataUpdater realtimeRaptorTransitDataUpdater,
    ConfigModel config,
    TimetableRepository timetableRepository
  ) {
    return new TimetableSnapshotManager(
      realtimeRaptorTransitDataUpdater,
      config.routerConfig().updaterConfig().timetableSnapshotParameters(),
      () -> LocalDate.now(timetableRepository.getTimeZone())
    );
  }

  /**
   * Create a single instance of the transit layer updater which holds the incremental caches for
   * the updates that need to applied to the {@link RaptorTransitData}.
   */
  @Provides
  @Singleton
  public static RealTimeRaptorTransitDataUpdater realtimeRaptorTransitDataUpdater(
    TimetableRepository timetableRepository
  ) {
    return new RealTimeRaptorTransitDataUpdater(timetableRepository);
  }

  /**
   * Provides the currently published, immutable {@link TimetableSnapshot}.
   */
  @Provides
  public static TimetableSnapshot timetableSnapshot(TimetableSnapshotManager manager) {
    return manager.getTimetableSnapshot();
  }

  /**
   * Provide the cache for both SIRI and GTFS-RT to make sure that trip patterns derived from
   * real time updates are not created repeatedly.
   */
  @Provides
  @Singleton
  public static TripPatternCache tripPatternCache(
    TimetableRepository repo,
    TimetableSnapshotManager manager
  ) {
    var service = new DefaultTransitService(repo, manager.getTimetableSnapshotBuffer());
    return new TripPatternCache(new TripPatternIdGenerator(), service::findPattern);
  }
}
