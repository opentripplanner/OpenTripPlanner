package org.opentripplanner.standalone.api;

import io.micrometer.core.instrument.Metrics;
import java.time.Duration;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.opentripplanner.core.framework.transaction.internal.TransactionFactory;
import org.opentripplanner.core.model.transaction.RepositoryHandle;
import org.opentripplanner.core.model.transaction.RepositoryRegistry;
import org.opentripplanner.ext.emission.internal.DefaultEmissionRepository;
import org.opentripplanner.ext.emission.internal.DefaultEmissionService;
import org.opentripplanner.ext.emission.internal.itinerary.EmissionItineraryDecorator;
import org.opentripplanner.graph_builder.issue.api.DataImportIssueStore;
import org.opentripplanner.place.api.NearbyStop;
import org.opentripplanner.raptor.configure.RaptorConfig;
import org.opentripplanner.routing.algorithm.filterchain.framework.spi.ItineraryDecorator;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.RaptorTransitData;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.TransitTuningParameters;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.TripSchedule;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.mappers.RaptorTransitDataMapper;
import org.opentripplanner.routing.api.RoutingService;
import org.opentripplanner.routing.api.request.RouteRequest;
import org.opentripplanner.routing.impl.TransitAlertServiceImpl;
import org.opentripplanner.routing.linking.LinkingContextFactory;
import org.opentripplanner.routing.linking.VertexLinkerTestFactory;
import org.opentripplanner.routing.linking.internal.VertexCreationService;
import org.opentripplanner.routing.service.DefaultRoutingService;
import org.opentripplanner.routing.via.ViaCoordinateTransferFactory;
import org.opentripplanner.routing.via.service.DefaultViaCoordinateTransferFactory;
import org.opentripplanner.service.streetdetails.StreetDetailsService;
import org.opentripplanner.service.streetdetails.internal.DefaultStreetDetailsRepository;
import org.opentripplanner.service.streetdetails.internal.DefaultStreetDetailsService;
import org.opentripplanner.service.vehicleparking.VehicleParkingService;
import org.opentripplanner.service.vehicleparking.internal.DefaultVehicleParkingRepository;
import org.opentripplanner.service.vehicleparking.internal.DefaultVehicleParkingService;
import org.opentripplanner.service.vehiclerental.VehicleRentalService;
import org.opentripplanner.service.vehiclerental.internal.DefaultVehicleRentalRepository;
import org.opentripplanner.service.vehiclerental.internal.DefaultVehicleRentalService;
import org.opentripplanner.service.worldenvelope.WorldEnvelopeService;
import org.opentripplanner.service.worldenvelope.internal.DefaultWorldEnvelopeRepository;
import org.opentripplanner.service.worldenvelope.internal.DefaultWorldEnvelopeService;
import org.opentripplanner.service.worldenvelope.model.WorldEnvelope;
import org.opentripplanner.standalone.config.RouterConfig;
import org.opentripplanner.standalone.config.routerconfig.RaptorEnvironmentFactory;
import org.opentripplanner.street.graph.Graph;
import org.opentripplanner.street.internal.DefaultStreetRepository;
import org.opentripplanner.street.linking.VertexLinker;
import org.opentripplanner.street.service.DefaultStreetLimitationParametersService;
import org.opentripplanner.street.service.StreetLimitationParametersService;
import org.opentripplanner.transfer.configure.StreetTransferFactory;
import org.opentripplanner.transfer.dataimport.RegularTransitTransferGenerator;
import org.opentripplanner.transfer.dataimport.StopsAllowedForModes;
import org.opentripplanner.transfer.dataimport.StopsVisitedByPatterns;
import org.opentripplanner.transfer.regular.TransferRepository;
import org.opentripplanner.transfer.regular.TransferServiceTestFactory;
import org.opentripplanner.transit.dataimport.TimetableDataImport;
import org.opentripplanner.transit.repository.DefaultTimetableRepository;
import org.opentripplanner.transit.repository.ScheduledTimetableData;
import org.opentripplanner.transit.repository.TimetableRepository;
import org.opentripplanner.transit.repository.TimetableRepositoryLifecycle;
import org.opentripplanner.transit.repository.TimetableRepositorySnapshot;
import org.opentripplanner.transit.service.DefaultTransitService;
import org.opentripplanner.transit.service.TransitRepository;
import org.opentripplanner.transit.service.TransitService;
import org.opentripplanner.transit.transfer.regular.NoRegularTransfers;
import org.opentripplanner.transit.transfer.regular.RegularTransferBuildRepository;
import org.opentripplanner.transit.transfer.regular.RegularTransferRepositorySnapshot;
import org.opentripplanner.transit.transfer.regular.configure.RegularTransferFactory;

public class TestServerContext {

  private TestServerContext() {}

  /**
   * Create a {@link TransitService} for unit testing: indexes the transit repository, builds
   * raptor transit data, and wraps a pinned timetable snapshot. Regular transfers are not part of
   * it, see {@link #createRegularTransfers}.
   */
  public static TransitService createTransitService(
    TransitRepository transitRepository,
    TimetableDataImport timetableDataImport,
    TransferRepository transferRepository
  ) {
    var registry = TransactionFactory.createRepositoryRegistry();
    var timetableHandle = indexAndRegisterTimetableSnapshot(
      transitRepository,
      timetableDataImport.toScheduledTimetableData(),
      transferRepository,
      registry
    );
    return new DefaultTransitService(
      transitRepository,
      timetableHandle.repositorySnapshot(registry.scope())
    );
  }

  private static RepositoryHandle<
    TimetableRepositorySnapshot,
    TimetableRepository
  > indexAndRegisterTimetableSnapshot(
    TransitRepository transitRepository,
    ScheduledTimetableData scheduledTimetableData,
    TransferRepository transferRepository,
    RepositoryRegistry registry
  ) {
    transitRepository.index();

    TransitTuningParameters tuningParameters = RouterConfig.DEFAULT.transitTuningConfig();
    var scheduledRaptorData = RaptorTransitDataMapper.map(
      tuningParameters,
      transitRepository,
      scheduledTimetableData,
      transferRepository
    );
    transitRepository.initRaptorTransitData(scheduledRaptorData);

    var timetableSnapshot = new DefaultTimetableRepository(
      new RaptorTransitData(transitRepository.getRaptorTransitData()),
      scheduledTimetableData
    );
    return registry.registerRepositorySnapshot(
      timetableSnapshot,
      new TimetableRepositoryLifecycle(timetableSnapshot, false, LocalDate::now)
    );
  }

  /**
   * Generate the regular transfers through the raptor-data pipeline, mirroring production graph
   * builds, register them on a new transaction registry, and return the current snapshot. The
   * transit repository must be indexed first, see {@link #createTransitService}.
   */
  public static RegularTransferRepositorySnapshot<NearbyStop, RouteRequest> createRegularTransfers(
    Graph graph,
    TransitRepository transitRepository,
    TimetableDataImport timetableDataImport
  ) {
    var registry = TransactionFactory.createRepositoryRegistry();
    var handle = StreetTransferFactory.registerRepository(
      registry,
      createRegularTransferBuildRepository(graph, transitRepository, timetableDataImport),
      StreetTransferFactory.defaultParameters(),
      graph,
      transitRepository,
      false
    );
    return handle.repositorySnapshot(registry.scope());
  }

  /**
   * Generate the regular transfers of a single default walk profile through the raptor-data
   * pipeline, mirroring production graph builds. The transit repository must be indexed first, see
   * {@link #createTransitService}.
   */
  public static RegularTransferBuildRepository<NearbyStop> createRegularTransferBuildRepository(
    Graph graph,
    TransitRepository transitRepository,
    TimetableDataImport timetableDataImport
  ) {
    RegularTransferBuildRepository<NearbyStop> regularTransferBuildRepository =
      RegularTransferFactory.createBuildRepository();
    var stopsVisitedByPatterns = new StopsVisitedByPatterns(timetableDataImport);
    var transferGenerator = StreetTransferFactory.createGenerator(
      graph,
      transitRepository,
      StreetTransferFactory.defaultParameters(),
      regularTransferBuildRepository,
      stopsVisitedByPatterns,
      new StopsAllowedForModes(timetableDataImport, stopsVisitedByPatterns),
      null
    );
    new RegularTransitTransferGenerator(
      () -> transferGenerator,
      graph,
      transitRepository,
      stopsVisitedByPatterns,
      DataImportIssueStore.NOOP
    ).buildGraph();
    return regularTransferBuildRepository;
  }

  /**
   * Create a {@link RoutingService} for unit testing. Pass {@link NoRegularTransfers#snapshot()} to
   * route without regular transfers.
   */
  public static RoutingService createRoutingService(
    Graph graph,
    TransitService transitService,
    TransferRepository transferRepository,
    RegularTransferRepositorySnapshot<NearbyStop, RouteRequest> regularTransfers
  ) {
    var routerConfig = RouterConfig.DEFAULT;
    var raptorConfig = createRaptorConfig();
    var vertexLinker = createVertexLinker(graph);

    return new DefaultRoutingService(
      transitService,
      graph,
      raptorConfig,
      Metrics.globalRegistry,
      createStreetLimitationParametersService(),
      createVehicleRentalService(),
      createStreetDetailsService(),
      TransferServiceTestFactory.transferService(transferRepository),
      regularTransfers,
      new TransitAlertServiceImpl(),
      routerConfig.flexParameters(),
      List.of(),
      null,
      null,
      createViaTransferResolver(graph, transitService),
      null,
      null,
      createEmissionsItineraryDecorator(),
      null,
      createLinkingContextFactory(graph, vertexLinker, transitService),
      routerConfig.transitTuningConfig(),
      routerConfig.transitTuningConfig(),
      null
    );
  }

  private static VertexLinker createVertexLinker(Graph graph) {
    return VertexLinkerTestFactory.of(graph);
  }

  public static RaptorConfig<TripSchedule> createRaptorConfig() {
    var routerConfig = RouterConfig.DEFAULT;
    return new RaptorConfig<>(
      routerConfig.transitTuningConfig(),
      RaptorEnvironmentFactory.create(routerConfig.transitTuningConfig().searchThreadPoolSize())
    );
  }

  /** Static factory method to create a service for test purposes. */
  public static WorldEnvelopeService createWorldEnvelopeService() {
    var repository = new DefaultWorldEnvelopeRepository();
    var envelope = WorldEnvelope.of()
      .expandToIncludeStreetEntities(0, 0)
      .expandToIncludeStreetEntities(1, 1)
      .build();
    repository.saveEnvelope(envelope);
    return new DefaultWorldEnvelopeService(repository);
  }

  public static VehicleRentalService createVehicleRentalService() {
    return new DefaultVehicleRentalService(new DefaultVehicleRentalRepository());
  }

  public static VehicleParkingService createVehicleParkingService() {
    return new DefaultVehicleParkingService(new DefaultVehicleParkingRepository());
  }

  public static ItineraryDecorator createEmissionsItineraryDecorator() {
    return new EmissionItineraryDecorator(
      new DefaultEmissionService(new DefaultEmissionRepository())
    );
  }

  public static StreetDetailsService createStreetDetailsService() {
    return new DefaultStreetDetailsService(new DefaultStreetDetailsRepository());
  }

  public static StreetLimitationParametersService createStreetLimitationParametersService() {
    return new DefaultStreetLimitationParametersService(new DefaultStreetRepository());
  }

  public static ViaCoordinateTransferFactory createViaTransferResolver(
    Graph graph,
    TransitService transitService
  ) {
    return new DefaultViaCoordinateTransferFactory(graph, transitService, Duration.ofMinutes(30));
  }

  public static LinkingContextFactory createLinkingContextFactory(
    Graph graph,
    VertexLinker vertexLinker,
    TransitService transitService
  ) {
    return new LinkingContextFactory(
      graph,
      new VertexCreationService(vertexLinker),
      transitService::findStopOrChildIds,
      id -> {
        var group = transitService.getStopLocationsGroup(id);
        return Optional.ofNullable(group).map(locationsGroup -> locationsGroup.getCoordinate());
      }
    );
  }
}
