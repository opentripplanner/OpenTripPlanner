package org.opentripplanner.transit.configure;

import dagger.Binds;
import dagger.Module;
import dagger.Provides;
import jakarta.inject.Singleton;
import java.time.LocalDate;
import org.opentripplanner.core.framework.transaction.configure.TransitDomain;
import org.opentripplanner.core.model.transaction.RepositoryHandle;
import org.opentripplanner.core.model.transaction.RepositoryRegistry;
import org.opentripplanner.framework.transaction.TimetableSnapshotParameters;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.RaptorTransitData;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.mappers.RaptorTransitDataMapper;
import org.opentripplanner.standalone.config.ConfigModel;
import org.opentripplanner.standalone.configure.RequestScopedFactory;
import org.opentripplanner.transfer.regular.TransferRepository;
import org.opentripplanner.transit.repository.DefaultTimetableRepository;
import org.opentripplanner.transit.repository.TimetableRepository;
import org.opentripplanner.transit.repository.TimetableRepositoryLifecycle;
import org.opentripplanner.transit.repository.TimetableRepositorySnapshot;
import org.opentripplanner.transit.service.DefaultTransitService;
import org.opentripplanner.transit.service.TransitRepository;
import org.opentripplanner.transit.service.TransitService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Module
public abstract class TransitModule {

  private static final Logger LOG = LoggerFactory.getLogger(TransitModule.class);

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

  /**
   * Maps the {@link RaptorTransitData} used to seed the realtime snapshot buffer. This must run
   * lazily, on first use, rather than eagerly at application-construction time: for an in-memory
   * {@code --build --serve} (or {@code --loadStreet --serve}) run, the transit repository is still
   * empty when the Dagger component is wired, and only gets populated afterwards by the graph
   * builder. Being a {@code @Provides} method (instead of a precomputed {@code @BindsInstance}),
   * Dagger only calls this the first time something needs the timetable repository handle, which
   * for every startup path happens after the graph is fully built and indexed.
   */
  @Provides
  @Singleton
  public static RepositoryHandle<
    TimetableRepositorySnapshot,
    TimetableRepository
  > timetableRepositoryHandle(
    TimetableSnapshotParameters parameters,
    ConfigModel config,
    TransitRepository transitRepository,
    TransferRepository transferRepository,
    @TransitDomain RepositoryRegistry repositoryRegistry
  ) {
    var tuningParameters = config.routerConfig().transitTuningConfig();
    if (!transitRepository.hasTransit() || !transitRepository.isIndexed()) {
      LOG.warn(
        "Cannot create Raptor data, that requires the graph to have transit data and be indexed."
      );
    }
    LOG.info("Creating transit layer for Raptor routing.");
    transitRepository.initRaptorTransitData(
      RaptorTransitDataMapper.map(tuningParameters, transitRepository, transferRepository)
    );
    var scheduledRaptorTransitData = new RaptorTransitData(
      transitRepository.getRaptorTransitData()
    );
    var tripCalendars = transitRepository.getTripCalendar();

    var buffer = new DefaultTimetableRepository(scheduledRaptorTransitData, tripCalendars);
    var lifecycle = new TimetableRepositoryLifecycle(buffer, parameters.purgeExpiredData(), () ->
      LocalDate.now(transitRepository.getTimeZone())
    );
    return repositoryRegistry.registerRepository(buffer, lifecycle);
  }
}
