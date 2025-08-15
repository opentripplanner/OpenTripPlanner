package org.opentripplanner.transit.configure;

import dagger.Binds;
import dagger.Module;
import dagger.Provides;
import jakarta.inject.Singleton;
import java.time.LocalDate;
import org.opentripplanner.model.TimetableSnapshot;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.RaptorTransitData;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.mappers.RealTimeRaptorTransitDataUpdater;
import org.opentripplanner.standalone.api.HttpRequestScoped;
import org.opentripplanner.standalone.config.ConfigModel;
import org.opentripplanner.transit.service.DefaultTransitService;
import org.opentripplanner.transit.service.TimetableRepository;
import org.opentripplanner.transit.service.TransitService;
import org.opentripplanner.updater.trip.TimetableSnapshotManager;

@Module
public abstract class TransitModule {

  @Binds
  @HttpRequestScoped
  abstract TransitService bind(DefaultTransitService service);

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
}
