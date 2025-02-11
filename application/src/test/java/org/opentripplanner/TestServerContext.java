package org.opentripplanner;

import static org.opentripplanner.standalone.configure.ConstructApplication.createRaptorTransitData;

import io.micrometer.core.instrument.Metrics;
import java.time.LocalDate;
import java.util.List;
import javax.annotation.Nullable;
import org.opentripplanner.ext.emissions.DefaultEmissionsService;
import org.opentripplanner.ext.emissions.EmissionsDataModel;
import org.opentripplanner.ext.emissions.EmissionsService;
import org.opentripplanner.raptor.configure.RaptorConfig;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.TripSchedule;
import org.opentripplanner.routing.api.request.RouteRequest;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.service.realtimevehicles.RealtimeVehicleService;
import org.opentripplanner.service.realtimevehicles.internal.DefaultRealtimeVehicleService;
import org.opentripplanner.service.vehicleparking.VehicleParkingService;
import org.opentripplanner.service.vehicleparking.internal.DefaultVehicleParkingRepository;
import org.opentripplanner.service.vehicleparking.internal.DefaultVehicleParkingService;
import org.opentripplanner.service.vehiclerental.VehicleRentalService;
import org.opentripplanner.service.vehiclerental.internal.DefaultVehicleRentalService;
import org.opentripplanner.service.worldenvelope.WorldEnvelopeService;
import org.opentripplanner.service.worldenvelope.internal.DefaultWorldEnvelopeRepository;
import org.opentripplanner.service.worldenvelope.internal.DefaultWorldEnvelopeService;
import org.opentripplanner.service.worldenvelope.model.WorldEnvelope;
import org.opentripplanner.standalone.api.OtpServerRequestContext;
import org.opentripplanner.standalone.config.DebugUiConfig;
import org.opentripplanner.standalone.config.RouterConfig;
import org.opentripplanner.standalone.config.routerconfig.RaptorEnvironmentFactory;
import org.opentripplanner.standalone.server.DefaultServerRequestContext;
import org.opentripplanner.street.model.StreetLimitationParameters;
import org.opentripplanner.street.service.DefaultStreetLimitationParametersService;
import org.opentripplanner.street.service.StreetLimitationParametersService;
import org.opentripplanner.transit.service.DefaultTransitService;
import org.opentripplanner.transit.service.TimetableRepository;
import org.opentripplanner.transit.service.TransitService;
import org.opentripplanner.updater.TimetableSnapshotParameters;
import org.opentripplanner.updater.trip.TimetableSnapshotManager;

public class TestServerContext {

  private TestServerContext() {}

  /** Create a context for unit testing using default RoutingRequest.*/
  public static OtpServerRequestContext createServerContext(
    Graph graph,
    TimetableRepository timetableRepository
  ) {
    return createServerContext(graph, timetableRepository, null, null);
  }

  /** Create a context for unit testing */
  public static OtpServerRequestContext createServerContext(
    Graph graph,
    TimetableRepository timetableRepository,
    @Nullable TimetableSnapshotManager snapshotManager,
    @Nullable RouteRequest request
  ) {
    var routerConfig = RouterConfig.DEFAULT;

    if (request == null) {
      request = routerConfig.routingRequestDefaults();
    }
    if (snapshotManager == null) {
      snapshotManager =
        new TimetableSnapshotManager(null, TimetableSnapshotParameters.DEFAULT, LocalDate::now);
    }

    timetableRepository.index();
    createRaptorTransitData(timetableRepository, routerConfig.transitTuningConfig());

    snapshotManager.purgeAndCommit();

    var transitService = new DefaultTransitService(
      timetableRepository,
      snapshotManager.getTimetableSnapshot()
    );

    var raptorConfig = new RaptorConfig<TripSchedule>(
      routerConfig.transitTuningConfig(),
      RaptorEnvironmentFactory.create(routerConfig.transitTuningConfig().searchThreadPoolSize())
    );

    return new DefaultServerRequestContext(
      DebugUiConfig.DEFAULT,
      routerConfig.flexParameters(),
      graph,
      Metrics.globalRegistry,
      raptorConfig,
      createRealtimeVehicleService(transitService),
      List.of(),
      request,
      createStreetLimitationParametersService(),
      routerConfig.transitTuningConfig(),
      transitService,
      routerConfig.vectorTileConfig(),
      createVehicleParkingService(),
      createVehicleRentalService(),
      createWorldEnvelopeService(),
      createEmissionsService(),
      null,
      null,
      null,
      null
    );
  }

  /** Static factory method to create a service for test purposes. */
  public static WorldEnvelopeService createWorldEnvelopeService() {
    var repository = new DefaultWorldEnvelopeRepository();
    var envelope = WorldEnvelope
      .of()
      .expandToIncludeStreetEntities(0, 0)
      .expandToIncludeStreetEntities(1, 1)
      .build();
    repository.saveEnvelope(envelope);
    return new DefaultWorldEnvelopeService(repository);
  }

  public static RealtimeVehicleService createRealtimeVehicleService(TransitService transitService) {
    return new DefaultRealtimeVehicleService(transitService);
  }

  public static VehicleRentalService createVehicleRentalService() {
    return new DefaultVehicleRentalService();
  }

  public static VehicleParkingService createVehicleParkingService() {
    return new DefaultVehicleParkingService(new DefaultVehicleParkingRepository());
  }

  public static EmissionsService createEmissionsService() {
    return new DefaultEmissionsService(new EmissionsDataModel());
  }

  public static StreetLimitationParametersService createStreetLimitationParametersService() {
    return new DefaultStreetLimitationParametersService(new StreetLimitationParameters());
  }
}
