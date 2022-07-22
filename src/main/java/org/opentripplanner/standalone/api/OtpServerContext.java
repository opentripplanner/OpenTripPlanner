package org.opentripplanner.standalone.api;

import io.micrometer.core.instrument.MeterRegistry;
import java.util.Locale;
import org.opentripplanner.inspector.TileRendererManager;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.TripSchedule;
import org.opentripplanner.routing.api.request.RoutingRequest;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.standalone.config.RouterConfig;
import org.opentripplanner.transit.raptor.configure.RaptorConfig;
import org.opentripplanner.transit.service.TransitModel;
import org.opentripplanner.visualizer.GraphVisualizer;
import org.slf4j.Logger;

/**
 * The purpose of this class is to allow APIs (HTTP Resources) to access the OTP Server Context.
 * By using an interface, and not inject the OTPServer class we avoid giving the resources assess
 * to the server implementation. The context is injected by Jersey. An alternative to inject this
 * interface is to inject each individual component in the context - hence reducing the dependencies
 * further. But there is not a "real" need for this. For example we do not have unit tests on the
 * Resources. If we in the future would decide to write unit tests for the APIs, then we could
 * eliminate this interface and just inject the components. See the bind method in OTPServer.
 */
public interface OtpServerContext {
  RoutingRequest copyDefaultRoutingRequest();

  Locale getDefaultLocale();

  double streetRoutingTimeoutSeconds();

  Graph graph();

  TransitModel transitModel();

  RouterConfig routerConfig();

  MeterRegistry meterRegistry();

  RaptorConfig<TripSchedule> raptorConfig();

  Logger requestLogger();

  TileRendererManager tileRendererManager();

  GraphVisualizer graphVisualizer();
}
