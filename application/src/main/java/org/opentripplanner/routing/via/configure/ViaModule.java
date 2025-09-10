package org.opentripplanner.routing.via.configure;

import dagger.Module;
import dagger.Provides;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.via.ViaCoordinateTransferFactory;
import org.opentripplanner.routing.via.service.DefaultViaCoordinateTransferFactory;
import org.opentripplanner.standalone.config.BuildConfig;
import org.opentripplanner.transit.service.TransitService;

@Module
public abstract class ViaModule {

  @Provides
  static ViaCoordinateTransferFactory providesViaTransferResolver(
    BuildConfig buildConfig,
    TransitService transitService,
    Graph graph
  ) {
    return new DefaultViaCoordinateTransferFactory(
      graph,
      transitService,
      buildConfig.maxTransferDuration
    );
  }
}
