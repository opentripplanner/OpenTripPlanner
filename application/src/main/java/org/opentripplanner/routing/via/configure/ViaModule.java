package org.opentripplanner.routing.via.configure;

import dagger.Module;
import dagger.Provides;
import org.opentripplanner.routing.via.ViaCoordinateTransferFactory;
import org.opentripplanner.routing.via.service.DefaultViaCoordinateTransferFactory;
import org.opentripplanner.standalone.config.BuildConfig;
import org.opentripplanner.street.graph.Graph;
import org.opentripplanner.transit.configure.StaticTransitService;
import org.opentripplanner.transit.service.TransitService;

@Module
public abstract class ViaModule {

  @Provides
  static ViaCoordinateTransferFactory providesViaTransferResolver(
    BuildConfig buildConfig,
    @StaticTransitService TransitService transitService,
    Graph graph
  ) {
    return new DefaultViaCoordinateTransferFactory(
      graph,
      transitService,
      buildConfig.regularTransferParameters().maxDuration()
    );
  }
}
