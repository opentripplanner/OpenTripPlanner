package org.opentripplanner.transit.model;

import static org.opentripplanner.core.model.id.FeedScopedIdForTestFactory.id;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import java.time.LocalDate;
import java.time.ZoneId;
import org.opentripplanner.LocalTimeParser;
import org.opentripplanner.core.model.id.FeedScopedIdForTestFactory;
import org.opentripplanner.framework.transaction.RepositoryRegistry;
import org.opentripplanner.framework.transaction.UpdateManager;
import org.opentripplanner.framework.transaction.api.RepositoryHandle;
import org.opentripplanner.framework.transaction.internal.TransactionFactory;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.RaptorTransitData;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.mappers.RaptorTransitDataMapper;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.request.DefaultTransitDataProviderFilterBuilder;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.request.RaptorRoutingRequestTransitData;
import org.opentripplanner.routing.api.request.RouteRequest;
import org.opentripplanner.transfer.regular.TransferRepository;
import org.opentripplanner.transit.model.network.grouppriority.TransitGroupPriorityService;
import org.opentripplanner.transit.repository.DefaultTimetableRepository;
import org.opentripplanner.transit.repository.TimetableRepository;
import org.opentripplanner.transit.repository.TimetableRepositoryLifecycle;
import org.opentripplanner.transit.repository.TimetableRepositorySnapshot;
import org.opentripplanner.transit.service.DefaultTransitService;
import org.opentripplanner.transit.service.TransitRepository;
import org.opentripplanner.transit.service.TransitService;
import org.opentripplanner.utils.time.ServiceDateUtils;

/**
 * A helper class for setting up and interacting with transit data for tests.
 * <p>
 * The builder is used to create a SiteRepository and a TransitRepository that can then be queried
 * by a TransitService.
 */
public final class TransitTestEnvironment {

  private final TransitRepository transitRepository;
  private final RepositoryRegistry repositoryRegistry;
  private final RepositoryHandle<TimetableRepositorySnapshot, TimetableRepository> timetableHandle;
  private final UpdateManager updateManager;
  private final LocalDate defaultServiceDate;

  public static TransitTestEnvironmentBuilder of() {
    return new TransitTestEnvironmentBuilder(ZoneId.of("Europe/Paris"), LocalDate.of(2024, 5, 7));
  }

  public static TransitTestEnvironmentBuilder of(LocalDate serviceDate) {
    return new TransitTestEnvironmentBuilder(ZoneId.of("Europe/Paris"), serviceDate);
  }

  public static TransitTestEnvironmentBuilder of(LocalDate serviceDate, ZoneId timeZone) {
    return new TransitTestEnvironmentBuilder(timeZone, serviceDate);
  }

  TransitTestEnvironment(
    TransitRepository transitRepository,
    TransferRepository transferRepository,
    LocalDate defaultServiceDate
  ) {
    this.transitRepository = transitRepository;
    this.defaultServiceDate = defaultServiceDate;

    this.transitRepository.index();
    var scheduledRaptorData = RaptorTransitDataMapper.map(
      new TestTransitTuningParameters(),
      transitRepository,
      transferRepository
    );
    this.transitRepository.initRaptorTransitData(scheduledRaptorData);

    this.repositoryRegistry = TransactionFactory.createRepositoryRegistry();
    var timetableSnapshot = new DefaultTimetableRepository(
      new RaptorTransitData(transitRepository.getRaptorTransitData()),
      transitRepository.copyTripCalendarForRealTimeUpdates()
    );
    this.timetableHandle = repositoryRegistry.registerRepositorySnapshot(
      timetableSnapshot,
      new TimetableRepositoryLifecycle(timetableSnapshot, false, () -> defaultServiceDate)
    );
    var threadFactory = new ThreadFactoryBuilder().setNameFormat("test-commit").build();
    // Use atomic commits (commit immediately after each task) so test assertions see results right away
    this.updateManager = TransactionFactory.createUpdateManagerWithAtomicCommits(
      "test",
      repositoryRegistry,
      threadFactory
    );
  }

  /**
   * The default service date is the same as the date used by the builder when creating trips when
   * no explicit date is specified.
   */
  public LocalDate defaultServiceDate() {
    return defaultServiceDate;
  }

  /**
   * Get the timezone of the timetable repository
   */
  public ZoneId timeZone() {
    return transitRepository.getTimeZone();
  }

  /**
   * Returns a new fresh TransitService backed by the current snapshot.
   */
  public TransitService transitService() {
    return new DefaultTransitService(transitRepository, timetableSnapshot());
  }

  public String feedId() {
    return FeedScopedIdForTestFactory.FEED_ID;
  }

  public TransitRepository transitRepository() {
    return transitRepository;
  }

  public RepositoryHandle<TimetableRepositorySnapshot, TimetableRepository> timetableHandle() {
    return timetableHandle;
  }

  public UpdateManager updateManager() {
    return updateManager;
  }

  public TimetableRepositorySnapshot timetableSnapshot() {
    return timetableHandle.repositorySnapshot(repositoryRegistry.scope());
  }

  /**
   * A parser for converting local times into absolute times on the default service date in the
   * TransitService timezone.
   */
  public LocalTimeParser localTimeParser() {
    return new LocalTimeParser(transitRepository.getTimeZone(), defaultServiceDate);
  }

  /**
   * Get a data fetcher for the given trip id on the default service date
   */
  public TripOnDateDataFetcher tripData(String tripId) {
    return new TripOnDateDataFetcher(transitService(), id(tripId), defaultServiceDate);
  }

  /**
   * Get a data fetcher for the given trip id on the given service date
   */
  public TripOnDateDataFetcher tripData(String tripId, LocalDate serviceDate) {
    return new TripOnDateDataFetcher(transitService(), id(tripId), serviceDate);
  }

  /**
   * Returns a fetcher for the given service date. By default it also includes cancelled trips.
   */
  public RaptorTransitDataFetcher raptorData(LocalDate serviceDate) {
    return new RaptorTransitDataFetcher(transitService(), serviceDate);
  }

  /**
   * Returns a fetcher for the default service date. By default it also includes cancelled trips.
   */
  public RaptorTransitDataFetcher raptorData() {
    return raptorData(defaultServiceDate);
  }

  /**
   * Returns {@link RaptorRoutingRequestTransitData} for the given service date.
   */
  public RaptorRoutingRequestTransitData raptorRoutingRequestTransitData(LocalDate serviceDate) {
    var transitSearchTimeZero = ServiceDateUtils.asStartOfService(
      serviceDate,
      transitRepository.getTimeZone()
    );
    return new RaptorRoutingRequestTransitData(
      transitRepository.getRaptorTransitData(),
      TransitGroupPriorityService.empty(),
      transitSearchTimeZero,
      0,
      0,
      new DefaultTransitDataProviderFilterBuilder().build(),
      RouteRequest.defaultValue()
    );
  }

  /**
   * Returns {@link RaptorRoutingRequestTransitData} for the default service date.
   */
  public RaptorRoutingRequestTransitData raptorRoutingRequestTransitData() {
    return raptorRoutingRequestTransitData(defaultServiceDate);
  }
}
