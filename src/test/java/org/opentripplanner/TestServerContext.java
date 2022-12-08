package org.opentripplanner;

import static org.opentripplanner.standalone.configure.ConstructApplication.creatTransitLayerForRaptor;

import io.micrometer.core.instrument.Metrics;
import org.opentripplanner.raptor.configure.RaptorConfig;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.standalone.api.OtpServerRequestContext;
import org.opentripplanner.standalone.config.RouterConfig;
import org.opentripplanner.standalone.server.DefaultServerRequestContext;
import org.opentripplanner.transit.service.DefaultTransitService;
import org.opentripplanner.transit.service.TransitModel;

public class TestServerContext {

  private TestServerContext() {}

  /** Create a context for unit testing, using the default RouteRequest. */
  public static OtpServerRequestContext createServerContext(
    Graph graph,
    TransitModel transitModel
  ) {
    transitModel.index();
    final RouterConfig routerConfig = RouterConfig.DEFAULT;
    DefaultServerRequestContext context = DefaultServerRequestContext.create(
      routerConfig.transitTuningConfig(),
      routerConfig.routingRequestDefaults(),
      routerConfig.streetRoutingTimeout(),
      new RaptorConfig<>(routerConfig.transitTuningConfig()),
      graph,
      new DefaultTransitService(transitModel),
      Metrics.globalRegistry,
      routerConfig.vectorTileLayers(),
      routerConfig.flexConfig(),
      null,
      routerConfig.requestLogFile()
    );
    creatTransitLayerForRaptor(transitModel, routerConfig.transitTuningConfig());
    return context;
  }
}
