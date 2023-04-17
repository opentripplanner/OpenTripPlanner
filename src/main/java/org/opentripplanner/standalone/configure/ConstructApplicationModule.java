package org.opentripplanner.standalone.configure;

import dagger.Module;
import dagger.Provides;
import io.micrometer.core.instrument.Metrics;
import java.util.List;
import javax.annotation.Nullable;
import org.opentripplanner.astar.spi.TraverseVisitor;
import org.opentripplanner.ext.ridehailing.RideHailingService;
import org.opentripplanner.raptor.configure.RaptorConfig;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.TripSchedule;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.service.vehiclepositions.VehiclePositionService;
import org.opentripplanner.service.vehiclerental.VehicleRentalService;
import org.opentripplanner.service.worldenvelope.WorldEnvelopeService;
import org.opentripplanner.standalone.api.OtpServerRequestContext;
import org.opentripplanner.standalone.config.RouterConfig;
import org.opentripplanner.standalone.server.DefaultServerRequestContext;
import org.opentripplanner.transit.service.TransitService;
import org.opentripplanner.visualizer.GraphVisualizer;

@Module
public class ConstructApplicationModule {

  @Provides
  OtpServerRequestContext providesServerContext(
    RouterConfig routerConfig,
    RaptorConfig<TripSchedule> raptorConfig,
    Graph graph,
    TransitService transitService,
    WorldEnvelopeService worldEnvelopeService,
    VehiclePositionService vehiclePositionService,
    VehicleRentalService vehicleRentalService,
    List<RideHailingService> rideHailingServices,
    @Nullable TraverseVisitor<?, ?> traverseVisitor
  ) {
    return DefaultServerRequestContext.create(
      routerConfig.transitTuningConfig(),
      routerConfig.routingRequestDefaults(),
      raptorConfig,
      graph,
      transitService,
      Metrics.globalRegistry,
      routerConfig.vectorTileLayers(),
      worldEnvelopeService,
      vehiclePositionService,
      vehicleRentalService,
      routerConfig.flexConfig(),
      rideHailingServices,
      traverseVisitor,
      routerConfig.requestLogFile()
    );
  }

  @Provides
  @Nullable
  TraverseVisitor<?, ?> traverseVisitor(@Nullable GraphVisualizer graphVisualizer) {
    return graphVisualizer == null ? null : graphVisualizer.traverseVisitor;
  }
}
