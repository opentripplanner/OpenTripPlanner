package org.opentripplanner.transit.configure;

import dagger.Binds;
import dagger.Module;
import dagger.Provides;
import jakarta.inject.Singleton;
import java.time.LocalDate;
import org.opentripplanner.framework.transaction.RepositoryRegistry;
import org.opentripplanner.framework.transaction.TimetableSnapshotParameters;
import org.opentripplanner.framework.transaction.api.RepositoryHandle;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.RaptorTransitData;
import org.opentripplanner.standalone.config.ConfigModel;
import org.opentripplanner.standalone.configure.RequestScopedFactory;
import org.opentripplanner.transit.model.calendar.DefaultTripCalendars;
import org.opentripplanner.transit.model.timetable.TimetableSnapshot;
import org.opentripplanner.transit.repository.MutableTimetableSnapshot;
import org.opentripplanner.transit.repository.ReadOnlyTimetableSnapshot;
import org.opentripplanner.transit.repository.TimetableSnapshotLifecycle;
import org.opentripplanner.transit.service.DefaultTransitService;
import org.opentripplanner.transit.service.TransitRepository;
import org.opentripplanner.transit.service.TransitService;

@Module
public abstract class TransitModule {

  /**
   * Binds the app-singleton, no-real-time-data {@link TransitService} used by consumers that live
   * outside any HTTP request (e.g. {@code DefaultRealtimeVehicleService}). The request-scoped,
   * snapshot-consistent {@link TransitService} is a distinct binding inside {@link
   * RequestScopedFactory} — do not retarget this one.
   */
  @Binds
  @StaticTransitService
  abstract TransitService bind(DefaultTransitService service);

  @Provides
  @Singleton
  public static TimetableSnapshotParameters timetableSnapshotParameters(ConfigModel config) {
    return config.routerConfig().updaterConfig().timetableSnapshotParameters();
  }

  @Provides
  @Singleton
  public static RepositoryHandle<
    ReadOnlyTimetableSnapshot,
    MutableTimetableSnapshot
  > timetableRepositoryHandle(
    TimetableSnapshotParameters parameters,
    TransitRepository transitRepository,
    RepositoryRegistry repositoryRegistry,
    RaptorTransitData scheduledRaptorTransitData,
    DefaultTripCalendars tripCalendars
  ) {
    var mutableBuffer = new TimetableSnapshot(scheduledRaptorTransitData, tripCalendars);
    var timetableSnapshotLifecycle = new TimetableSnapshotLifecycle(
      mutableBuffer,
      parameters.purgeExpiredData(),
      () -> LocalDate.now(transitRepository.getTimeZone())
    );
    return repositoryRegistry.registerRepository(mutableBuffer, timetableSnapshotLifecycle);
  }
}
