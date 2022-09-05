package org.opentripplanner.standalone.server;

import io.micrometer.core.instrument.MeterRegistry;
import java.util.Locale;
import javax.annotation.Nullable;
import org.opentripplanner.inspector.TileRendererManager;
import org.opentripplanner.routing.RoutingService;
import org.opentripplanner.routing.algorithm.astar.TraverseVisitor;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.TripSchedule;
import org.opentripplanner.routing.api.request.RouteRequest;
import org.opentripplanner.routing.api.request.preference.RoutingPreferences;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.standalone.api.HttpRequestScoped;
import org.opentripplanner.standalone.api.OtpServerRequestContext;
import org.opentripplanner.standalone.config.RouterConfig;
import org.opentripplanner.standalone.configure.RequestLoggerFactory;
import org.opentripplanner.transit.raptor.configure.RaptorConfig;
import org.opentripplanner.transit.service.TransitService;
import org.slf4j.Logger;

@HttpRequestScoped
public class DefaultServerRequestContext implements OtpServerRequestContext {

  private RouteRequest routeRequest = null;
  private final Graph graph;
  private final TransitService transitService;
  private final RouterConfig routerConfig;
  private final MeterRegistry meterRegistry;
  private final RaptorConfig<TripSchedule> raptorConfig;
  public final Logger requestLogger;
  private final TileRendererManager tileRendererManager;
  public final TraverseVisitor traverseVisitor;

  /**
   * Make sure all mutable components are copied/cloned before calling this constructor.
   */
  private DefaultServerRequestContext(
    Graph graph,
    TransitService transitService,
    RouterConfig routerConfig,
    MeterRegistry meterRegistry,
    RaptorConfig<TripSchedule> raptorConfig,
    Logger requestLogger,
    TileRendererManager tileRendererManager,
    TraverseVisitor traverseVisitor
  ) {
    this.graph = graph;
    this.transitService = transitService;
    this.routerConfig = routerConfig;
    this.meterRegistry = meterRegistry;
    this.raptorConfig = raptorConfig;
    this.requestLogger = requestLogger;
    this.tileRendererManager = tileRendererManager;
    this.traverseVisitor = traverseVisitor;
  }

  /**
   * Create a server context valid for one http request only!
   */
  public static DefaultServerRequestContext create(
    RouterConfig routerConfig,
    RaptorConfig<TripSchedule> raptorConfig,
    Graph graph,
    TransitService transitService,
    MeterRegistry meterRegistry,
    @Nullable TraverseVisitor traverseVisitor
  ) {
    var defaultRoutingPreferences = routerConfig.routingRequestDefaults().preferences();

    return new DefaultServerRequestContext(
      graph,
      transitService,
      routerConfig,
      meterRegistry,
      raptorConfig,
      RequestLoggerFactory.createLogger(routerConfig.requestLogFile()),
      new TileRendererManager(graph, defaultRoutingPreferences),
      traverseVisitor
    );
  }

  // TODO VIA: Create assert that this is never mutated (even in deeply nested objects)
  @Override
  public RouteRequest defaultRouteRequest() {
    // Lazy initialize request-scoped request to avoid doing this when not needed
    if (routeRequest == null) {
      routeRequest = routerConfig.routingRequestDefaults().copyWithDateTimeNow();
    }
    return routeRequest;
  }

  /**
   * Return the default routing request locale(without cloning the request).
   */
  @Override
  public Locale defaultLocale() {
    return routerConfig().routingRequestDefaults().locale();
  }

  @Override
  public RouterConfig routerConfig() {
    return routerConfig;
  }

  @Override
  public RaptorConfig<TripSchedule> raptorConfig() {
    return raptorConfig;
  }

  @Override
  public Graph graph() {
    return graph;
  }

  @Override
  public TransitService transitService() {
    return transitService;
  }

  @Override
  public RoutingService routingService() {
    return new RoutingService(this);
  }

  @Override
  public MeterRegistry meterRegistry() {
    return meterRegistry;
  }

  @Override
  public Logger requestLogger() {
    return requestLogger;
  }

  @Override
  public TileRendererManager tileRendererManager() {
    return tileRendererManager;
  }

  @Override
  public TraverseVisitor traverseVisitor() {
    return traverseVisitor;
  }
}
