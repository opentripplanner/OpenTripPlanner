package org.opentripplanner.standalone.configure;

import dagger.Module;
import dagger.Provides;
import io.micrometer.core.instrument.Metrics;
import java.util.List;
import javax.annotation.Nullable;
import org.opentripplanner.astar.spi.TraverseVisitor;
import org.opentripplanner.ext.debugrastertiles.TileRendererManager;
import org.opentripplanner.ext.emissions.EmissionsService;
import org.opentripplanner.ext.geocoder.LuceneIndex;
import org.opentripplanner.ext.interactivelauncher.api.LauncherRequestDecorator;
import org.opentripplanner.ext.ridehailing.RideHailingService;
import org.opentripplanner.ext.sorlandsbanen.SorlandsbanenNorwayService;
import org.opentripplanner.ext.stopconsolidation.StopConsolidationService;
import org.opentripplanner.raptor.configure.RaptorConfig;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.TripSchedule;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.service.realtimevehicles.RealtimeVehicleService;
import org.opentripplanner.service.vehicleparking.VehicleParkingService;
import org.opentripplanner.service.vehiclerental.VehicleRentalService;
import org.opentripplanner.service.worldenvelope.WorldEnvelopeService;
import org.opentripplanner.standalone.api.OtpServerRequestContext;
import org.opentripplanner.standalone.config.DebugUiConfig;
import org.opentripplanner.standalone.config.RouterConfig;
import org.opentripplanner.standalone.server.DefaultServerRequestContext;
import org.opentripplanner.street.service.StreetLimitationParametersService;
import org.opentripplanner.transit.service.TransitService;
import org.opentripplanner.visualizer.GraphVisualizer;

@Module
public class ConstructApplicationModule {

  @Provides
  OtpServerRequestContext providesServerContext(
    RouterConfig routerConfig,
    DebugUiConfig debugUiConfig,
    RaptorConfig<TripSchedule> raptorConfig,
    Graph graph,
    TransitService transitService,
    WorldEnvelopeService worldEnvelopeService,
    RealtimeVehicleService realtimeVehicleService,
    VehicleRentalService vehicleRentalService,
    VehicleParkingService vehicleParkingService,
    List<RideHailingService> rideHailingServices,
    @Nullable StopConsolidationService stopConsolidationService,
    StreetLimitationParametersService streetLimitationParametersService,
    @Nullable TraverseVisitor<?, ?> traverseVisitor,
    EmissionsService emissionsService,
    @Nullable SorlandsbanenNorwayService sorlandsbanenService,
    LauncherRequestDecorator launcherRequestDecorator,
    @Nullable LuceneIndex luceneIndex
  ) {
    var defaultRequest = launcherRequestDecorator.intercept(routerConfig.routingRequestDefaults());

    var transitRoutingConfig = routerConfig.transitTuningConfig();
    var vectorTileConfig = routerConfig.vectorTileConfig();
    var flexParameters = routerConfig.flexParameters();

    // TODO: Inject this, can this use the routerConfig routingRequest ?
    var tileRendererManager = new TileRendererManager(graph, defaultRequest.preferences());

    return new DefaultServerRequestContext(
      debugUiConfig,
      flexParameters,
      graph,
      Metrics.globalRegistry,
      raptorConfig,
      realtimeVehicleService,
      rideHailingServices,
      defaultRequest,
      streetLimitationParametersService,
      transitRoutingConfig,
      transitService,
      vectorTileConfig,
      vehicleParkingService,
      vehicleRentalService,
      worldEnvelopeService,
      emissionsService,
      luceneIndex,
      sorlandsbanenService,
      stopConsolidationService,
      tileRendererManager,
      traverseVisitor
    );
  }

  @Provides
  @Nullable
  TraverseVisitor<?, ?> traverseVisitor(@Nullable GraphVisualizer graphVisualizer) {
    return graphVisualizer == null ? null : graphVisualizer.traverseVisitor;
  }
}
