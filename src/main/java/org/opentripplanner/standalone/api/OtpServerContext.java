package org.opentripplanner.standalone.api;

import io.micrometer.core.instrument.MeterRegistry;
import java.util.Locale;
import org.opentripplanner.inspector.TileRendererManager;
import org.opentripplanner.routing.RoutingService;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.TripSchedule;
import org.opentripplanner.routing.api.request.RoutingRequest;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.standalone.config.RouterConfig;
import org.opentripplanner.transit.raptor.configure.RaptorConfig;
import org.opentripplanner.transit.service.TransitModel;
import org.opentripplanner.transit.service.TransitService;
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

  /**
   * Return the default routing request locale(without cloning the request).
   */
  Locale getDefaultLocale();

  double streetRoutingTimeoutSeconds();

  Graph graph();

  TransitModel transitModel();

  RouterConfig routerConfig();

  MeterRegistry meterRegistry();

  RaptorConfig<TripSchedule> raptorConfig();

  /**
   * Separate logger for incoming requests. This should be handled with a Logback logger rather than
   * something simple like a PrintStream because requests come in multi-threaded.
   */
  Logger requestLogger();

  /** Inspector/debug services */
  TileRendererManager tileRendererManager();

  /**
   * A graphical window that is used for visualizing search progress (debugging).
   */
  GraphVisualizer graphVisualizer();

  /**
   * This method is used to create a {@link RoutingService} valid for one request. It grantees that
   * the data and services used are consistent and operate on the same transit snapshot. Any
   * realtime update that happens during the request will not affect the returned service and will
   * not be visible to the request.
   */
  RoutingService routingRequestService();

  TransitService transitRequestService();
}
