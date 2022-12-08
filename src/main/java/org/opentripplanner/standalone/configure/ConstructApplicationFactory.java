package org.opentripplanner.standalone.configure;

import dagger.BindsInstance;
import dagger.Component;
import javax.annotation.Nullable;
import javax.inject.Singleton;
import org.opentripplanner.raptor.configure.RaptorConfig;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.TripSchedule;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.service.worldenvelope.configure.WorldEnvelopeModule;
import org.opentripplanner.service.worldenvelope.service.WorldEnvelopeModel;
import org.opentripplanner.standalone.api.OtpServerRequestContext;
import org.opentripplanner.standalone.config.ConfigModel;
import org.opentripplanner.standalone.config.configure.ConfigModule;
import org.opentripplanner.standalone.server.MetricsLogging;
import org.opentripplanner.transit.configure.TransitModule;
import org.opentripplanner.transit.service.TransitModel;
import org.opentripplanner.transit.service.TransitService;
import org.opentripplanner.visualizer.GraphVisualizer;

/**
 * Dagger dependency injection Factory to create components for the OTP construct application phase.
 */
@Singleton
@Component(
  modules = {
    ConfigModule.class,
    TransitModule.class,
    ConstructApplicationModule.class,
    WorldEnvelopeModule.class,
  }
)
public interface ConstructApplicationFactory {
  ConfigModel config();
  RaptorConfig<TripSchedule> raptorConfig();
  Graph graph();
  TransitModel transitModel();
  WorldEnvelopeModel worldEnvelopeModel();

  @Nullable
  GraphVisualizer graphVisualizer();

  TransitService transitService();
  OtpServerRequestContext createServerContext();

  MetricsLogging metricsLogging();

  @Component.Builder
  interface Builder {
    @BindsInstance
    Builder configModel(ConfigModel config);

    @BindsInstance
    Builder graph(Graph graph);

    @BindsInstance
    Builder transitModel(TransitModel transitModel);

    @BindsInstance
    Builder graphVisualizer(@Nullable GraphVisualizer graphVisualizer);

    @BindsInstance
    Builder worldEnvelopeModel(WorldEnvelopeModel worldEnvelopeModel);

    ConstructApplicationFactory build();
  }
}
