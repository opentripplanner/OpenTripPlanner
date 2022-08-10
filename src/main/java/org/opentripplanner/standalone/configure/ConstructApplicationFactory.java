package org.opentripplanner.standalone.configure;

import dagger.BindsInstance;
import dagger.Component;
import javax.inject.Singleton;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.TripSchedule;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.standalone.config.ConfigModel;
import org.opentripplanner.standalone.config.configure.ConfigModule;
import org.opentripplanner.transit.configure.TransitModule;
import org.opentripplanner.transit.raptor.configure.RaptorConfig;
import org.opentripplanner.transit.service.TransitModel;
import org.opentripplanner.transit.service.TransitService;
import org.opentripplanner.visualizer.GraphVisualizer;

/**
 * Dagger dependency injection Factory to create components for the OTP construct application phase.
 */
@Singleton
@Component(modules = { ConfigModule.class, TransitModule.class })
public interface ConstructApplicationFactory {
  ConfigModel config();
  RaptorConfig<TripSchedule> raptorConfig();
  Graph graph();
  TransitModel transitModel();
  GraphVisualizer graphVisualizer();
  TransitService transitService();

  @Component.Builder
  interface Builder {
    @BindsInstance
    Builder configModel(ConfigModel config);

    @BindsInstance
    Builder graph(Graph graph);

    @BindsInstance
    Builder transitModel(TransitModel transitModel);

    ConstructApplicationFactory build();
  }
}
