package org.opentripplanner.standalone.configure;

import dagger.BindsInstance;
import dagger.Component;
import javax.inject.Singleton;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.TripSchedule;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.standalone.config.ConfigModel;
import org.opentripplanner.transit.raptor.configure.RaptorConfig;
import org.opentripplanner.visualizer.GraphVisualizer;

/**
 * Dagger dependency injection Factory to create components for the OTP construct application phase.
 */
@Singleton
@Component(modules = ConstructApplicationModule.class)
public interface ConstructApplicationFactory {
  RaptorConfig<TripSchedule> raptorConfig();

  GraphVisualizer graphVisualizer();

  @Component.Builder
  interface Builder {
    @BindsInstance
    Builder configModel(ConfigModel config);

    @BindsInstance
    Builder graph(Graph graph);

    ConstructApplicationFactory build();
  }
}
