package org.opentripplanner.standalone.api;

import io.micrometer.core.instrument.Metrics;
import java.time.Duration;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import javax.annotation.Nullable;
import org.opentripplanner.ext.emission.internal.DefaultEmissionRepository;
import org.opentripplanner.ext.emission.internal.DefaultEmissionService;
import org.opentripplanner.ext.emission.internal.itinerary.EmissionItineraryDecorator;
import org.opentripplanner.ext.fares.service.gtfs.v1.DefaultFareService;
import org.opentripplanner.ext.flex.FlexParameters;
import org.opentripplanner.framework.transaction.api.RepositoryHandle;
import org.opentripplanner.framework.transaction.internal.TransactionFactory;
import org.opentripplanner.raptor.configure.RaptorConfig;
import org.opentripplanner.routing.algorithm.filterchain.framework.spi.ItineraryDecorator;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.RaptorTransitData;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.TransitTuningParameters;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.TripSchedule;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.mappers.RaptorTransitDataMapper;
import org.opentripplanner.routing.api.request.RouteRequest;
import org.opentripplanner.routing.fares.FareService;
import org.opentripplanner.routing.linking.LinkingContextFactory;
import org.opentripplanner.routing.linking.VertexLinkerTestFactory;
import org.opentripplanner.routing.linking.internal.VertexCreationService;
import org.opentripplanner.routing.via.ViaCoordinateTransferFactory;
import org.opentripplanner.routing.via.service.DefaultViaCoordinateTransferFactory;
import org.opentripplanner.service.realtimevehicles.internal.DefaultRealtimeVehicleRepository;
import org.opentripplanner.service.streetdetails.StreetDetailsService;
import org.opentripplanner.service.streetdetails.internal.DefaultStreetDetailsRepository;
import org.opentripplanner.service.streetdetails.internal.DefaultStreetDetailsService;
import org.opentripplanner.service.vehicleparking.VehicleParkingService;
import org.opentripplanner.service.vehicleparking.internal.DefaultVehicleParkingRepository;
import org.opentripplanner.service.vehicleparking.internal.DefaultVehicleParkingService;
import org.opentripplanner.service.vehiclerental.VehicleRentalService;
import org.opentripplanner.service.vehiclerental.internal.DefaultVehicleRentalService;
import org.opentripplanner.service.worldenvelope.WorldEnvelopeService;
import org.opentripplanner.service.worldenvelope.internal.DefaultWorldEnvelopeRepository;
import org.opentripplanner.service.worldenvelope.internal.DefaultWorldEnvelopeService;
import org.opentripplanner.service.worldenvelope.model.WorldEnvelope;
import org.opentripplanner.standalone.config.DebugUiConfig;
import org.opentripplanner.standalone.config.RouterConfig;
import org.opentripplanner.standalone.config.routerconfig.RaptorEnvironmentFactory;
import org.opentripplanner.standalone.server.DefaultServerRequestContext;
import org.opentripplanner.street.graph.Graph;
import org.opentripplanner.street.internal.DefaultStreetRepository;
import org.opentripplanner.street.linking.VertexLinker;
import org.opentripplanner.street.service.DefaultStreetLimitationParametersService;
import org.opentripplanner.street.service.StreetLimitationParametersService;
import org.opentripplanner.transfer.regular.TransferRepository;
import org.opentripplanner.transfer.regular.TransferServiceTestFactory;
import org.opentripplanner.transfer.regular.internal.DefaultTransferRepository;
import org.opentripplanner.transfer.regular.internal.TransferIndex;
import org.opentripplanner.transit.model.timetable.TimetableSnapshot;
import org.opentripplanner.transit.repository.MutableTimetableSnapshot;
import org.opentripplanner.transit.repository.ReadOnlyTimetableSnapshot;
import org.opentripplanner.transit.repository.TimetableSnapshotLifecycle;
import org.opentripplanner.transit.service.DefaultTransitService;
import org.opentripplanner.transit.service.TransitRepository;
import org.opentripplanner.transit.service.TransitService;

public class TestServerContext {

  private TestServerContext() {}

  /** Create a context for unit testing using default RoutingRequest. */
  public static OtpServerRequestContext createServerContext(
    Graph graph,
    TransitRepository transitRepository,
    TransferRepository transferRepository,
    FareService fareService
  ) {
    return createServerContext(
      graph,
      transitRepository,
      transferRepository,
      fareService,
      null,
      null
    );
  }

  /** Create a context for unit testing */
  public static OtpServerRequestContext createServerContext(
    Graph graph,
    TransitRepository transitRepository,
    TransferRepository transferRepository,
    FareService fareService,
    @Nullable RouteRequest request,
    @Nullable FlexParameters flexParameters
  ) {
    var routerConfig = RouterConfig.DEFAULT;

    if (request == null) {
      request = routerConfig.routingRequestDefaults();
    }
    if (flexParameters == null) {
      flexParameters = routerConfig.flexParameters();
    }
    transitRepository.index();

    TransitTuningParameters tuningParameters = routerConfig.transitTuningConfig();
    var scheduledRaptorData = RaptorTransitDataMapper.map(
      tuningParameters,
      transitRepository,
      transferRepository
    );
    transitRepository.initRaptorTransitData(scheduledRaptorData);

    var registry = TransactionFactory.createRepositoryRegistry();
    var timetableSnapshot = new TimetableSnapshot(
      new RaptorTransitData(transitRepository.getRaptorTransitData()),
      transitRepository.copyTripCalendarForRealTimeUpdates()
    );
    RepositoryHandle<ReadOnlyTimetableSnapshot, MutableTimetableSnapshot> timetableHandle =
      registry.registerRepositorySnapshot(
        timetableSnapshot,
        new TimetableSnapshotLifecycle(timetableSnapshot, false, LocalDate::now)
      );

    return buildContext(
      graph,
      transitRepository,
      transferRepository,
      fareService,
      request,
      flexParameters,
      routerConfig,
      registry,
      timetableHandle
    );
  }

  /**
   * Create a context for unit testing using an existing repository handle (e.g. when real-time
   * updates have already been applied to that handle before context creation).
   */
  public static OtpServerRequestContext createServerContext(
    Graph graph,
    TransitRepository transitRepository,
    TransferRepository transferRepository,
    FareService fareService,
    RepositoryHandle<ReadOnlyTimetableSnapshot, MutableTimetableSnapshot> timetableHandle,
    org.opentripplanner.framework.transaction.RepositoryRegistry registry,
    @Nullable RouteRequest request,
    @Nullable FlexParameters flexParameters
  ) {
    var routerConfig = RouterConfig.DEFAULT;
    if (request == null) {
      request = routerConfig.routingRequestDefaults();
    }
    if (flexParameters == null) {
      flexParameters = routerConfig.flexParameters();
    }
    return buildContext(
      graph,
      transitRepository,
      transferRepository,
      fareService,
      request,
      flexParameters,
      routerConfig,
      registry,
      timetableHandle
    );
  }

  private static OtpServerRequestContext buildContext(
    Graph graph,
    TransitRepository transitRepository,
    TransferRepository transferRepository,
    FareService fareService,
    RouteRequest request,
    FlexParameters flexParameters,
    RouterConfig routerConfig,
    org.opentripplanner.framework.transaction.RepositoryRegistry registry,
    RepositoryHandle<ReadOnlyTimetableSnapshot, MutableTimetableSnapshot> timetableHandle
  ) {
    var transactionScope = registry.scope();
    var transitService = new DefaultTransitService(
      transitRepository,
      timetableHandle.repositorySnapshot(transactionScope)
    );

    var raptorConfig = new RaptorConfig<TripSchedule>(
      routerConfig.transitTuningConfig(),
      RaptorEnvironmentFactory.create(routerConfig.transitTuningConfig().searchThreadPoolSize())
    );

    var vertexLinker = createVertexLinker(graph);

    return new DefaultServerRequestContext(
      DebugUiConfig.DEFAULT,
      fareService,
      flexParameters,
      graph,
      createLinkingContextFactory(graph, vertexLinker, transitService),
      Metrics.globalRegistry,
      routerConfig.ojpApiParameters(),
      raptorConfig,
      new DefaultRealtimeVehicleRepository(),
      List.of(),
      request,
      createStreetLimitationParametersService(),
      TransferServiceTestFactory.transferService(transferRepository),
      transactionScope,
      routerConfig.transitTuningConfig(),
      transitService,
      routerConfig.triasApiParameters(),
      routerConfig.gtfsApiParameters(),
      routerConfig.vectorTileConfig(),
      createVehicleParkingService(),
      createVehicleRentalService(),
      vertexLinker,
      createViaTransferResolver(graph, transitService),
      createWorldEnvelopeService(),
      null,
      null,
      createEmissionsItineraryDecorator(),
      createStreetDetailsService(),
      null,
      null,
      null,
      null,
      null,
      null,
      null
    );
  }

  private static VertexLinker createVertexLinker(Graph graph) {
    return VertexLinkerTestFactory.of(graph);
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
    return new DefaultVehicleRentalService();
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

  public static OtpServerRequestContext ofGraph(Graph graph) {
    return createServerContext(
      graph,
      new TransitRepository(),
      new DefaultTransferRepository(new TransferIndex()),
      new DefaultFareService()
    );
  }
}
