package org.opentripplanner.standalone.configure;

import dagger.Module;
import dagger.Provides;
import io.micrometer.core.instrument.Metrics;
import javax.annotation.Nullable;
import org.opentripplanner.routing.algorithm.astar.TraverseVisitor;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.TripSchedule;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.standalone.api.OtpServerRequestContext;
import org.opentripplanner.standalone.config.RouterConfig;
import org.opentripplanner.standalone.server.DefaultServerRequestContext;
import org.opentripplanner.transit.raptor.configure.RaptorConfig;
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
    @Nullable TraverseVisitor traverseVisitor
  ) {
    return DefaultServerRequestContext.create(
      routerConfig,
      raptorConfig,
      graph,
      transitService,
      Metrics.globalRegistry,
      traverseVisitor
    );
  }

  @Provides
  @Nullable
  TraverseVisitor traverseVisitor(@Nullable GraphVisualizer graphVisualizer) {
    return graphVisualizer == null ? null : graphVisualizer.traverseVisitor;
  }
}
