package org.opentripplanner;

import static org.opentripplanner.standalone.configure.ConstructApplication.creatTransitLayerForRaptor;

import io.micrometer.core.instrument.Metrics;
import java.util.List;
import org.opentripplanner.ext.emissions.DefaultEmissionsService;
import org.opentripplanner.ext.emissions.EmissionsDataModel;
import org.opentripplanner.ext.emissions.EmissionsService;
import org.opentripplanner.raptor.configure.RaptorConfig;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.service.realtimevehicles.RealtimeVehicleService;
import org.opentripplanner.service.realtimevehicles.internal.DefaultRealtimeVehicleService;
import org.opentripplanner.service.vehiclerental.VehicleRentalService;
import org.opentripplanner.service.vehiclerental.internal.DefaultVehicleRentalService;
import org.opentripplanner.service.worldenvelope.WorldEnvelopeService;
import org.opentripplanner.service.worldenvelope.internal.DefaultWorldEnvelopeRepository;
import org.opentripplanner.service.worldenvelope.internal.DefaultWorldEnvelopeService;
import org.opentripplanner.service.worldenvelope.model.WorldEnvelope;
import org.opentripplanner.standalone.api.OtpServerRequestContext;
import org.opentripplanner.standalone.config.RouterConfig;
import org.opentripplanner.standalone.config.routerconfig.RaptorEnvironmentFactory;
import org.opentripplanner.standalone.server.DefaultServerRequestContext;
import org.opentripplanner.street.model.StreetLimitationParameters;
import org.opentripplanner.street.service.DefaultStreetLimitationParametersService;
import org.opentripplanner.street.service.StreetLimitationParametersService;
import org.opentripplanner.transit.service.DefaultTransitService;
import org.opentripplanner.transit.service.TimetableRepository;
import org.opentripplanner.transit.service.TransitService;

public class TestServerContext {

  private TestServerContext() {}

  /** Create a context for unit testing, using the default RouteRequest. */
  public static OtpServerRequestContext createServerContext(
    Graph graph,
    TimetableRepository timetableRepository
  ) {
    timetableRepository.index();
    final RouterConfig routerConfig = RouterConfig.DEFAULT;
    var transitService = new DefaultTransitService(timetableRepository);
    DefaultServerRequestContext context = DefaultServerRequestContext.create(
      routerConfig.transitTuningConfig(),
      routerConfig.routingRequestDefaults(),
      new RaptorConfig<>(
        routerConfig.transitTuningConfig(),
        RaptorEnvironmentFactory.create(routerConfig.transitTuningConfig().searchThreadPoolSize())
      ),
      graph,
      new DefaultTransitService(timetableRepository),
      Metrics.globalRegistry,
      routerConfig.vectorTileConfig(),
      createWorldEnvelopeService(),
      createRealtimeVehicleService(transitService),
      createVehicleRentalService(),
      createEmissionsService(),
      null,
      routerConfig.flexParameters(),
      List.of(),
      null,
      createStreetLimitationParametersService(),
      null,
      null
    );
    creatTransitLayerForRaptor(timetableRepository, routerConfig.transitTuningConfig());
    return context;
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

  public static EmissionsService createEmissionsService() {
    return new DefaultEmissionsService(new EmissionsDataModel());
  }

  public static StreetLimitationParametersService createStreetLimitationParametersService() {
    return new DefaultStreetLimitationParametersService(new StreetLimitationParameters());
  }
}
