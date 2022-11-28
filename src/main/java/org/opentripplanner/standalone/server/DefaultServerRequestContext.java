package org.opentripplanner.standalone.server;

import io.micrometer.core.instrument.MeterRegistry;
import java.time.Duration;
import java.util.Locale;
import javax.annotation.Nullable;
import org.opentripplanner.astar.spi.TraverseVisitor;
import org.opentripplanner.ext.vectortiles.VectorTilesResource;
import org.opentripplanner.inspector.TileRendererManager;
import org.opentripplanner.raptor.api.request.RaptorTuningParameters;
import org.opentripplanner.raptor.configure.RaptorConfig;
import org.opentripplanner.routing.RoutingService;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.TransitTuningParameters;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.TripSchedule;
import org.opentripplanner.routing.api.request.RouteRequest;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.standalone.api.HttpRequestScoped;
import org.opentripplanner.standalone.api.OtpServerRequestContext;
import org.opentripplanner.standalone.config.routerconfig.TransitRoutingConfig;
import org.opentripplanner.standalone.config.sandbox.FlexConfig;
import org.opentripplanner.standalone.configure.RequestLoggerFactory;
import org.opentripplanner.transit.service.TransitService;
import org.slf4j.Logger;

@HttpRequestScoped
public class DefaultServerRequestContext implements OtpServerRequestContext {

  private RouteRequest routeRequest = null;
  private final Graph graph;
  private final TransitService transitService;
  private final TransitRoutingConfig transitRoutingConfig;
  private final Duration streetRoutingTimeout;
  private final RouteRequest routeRequestDefaults;
  private final MeterRegistry meterRegistry;
  private final RaptorConfig<TripSchedule> raptorConfig;
  private final Logger requestLogger;
  private final TileRendererManager tileRendererManager;
  private final VectorTilesResource.LayersParameters vectorTileLayers;
  private final FlexConfig flexConfig;
  private final TraverseVisitor traverseVisitor;

  /**
   * Make sure all mutable components are copied/cloned before calling this constructor.
   */
  private DefaultServerRequestContext(
    Graph graph,
    TransitService transitService,
    TransitRoutingConfig transitRoutingConfig,
    Duration streetRoutingTimeout,
    RouteRequest routeRequestDefaults,
    MeterRegistry meterRegistry,
    RaptorConfig<TripSchedule> raptorConfig,
    Logger requestLogger,
    TileRendererManager tileRendererManager,
    VectorTilesResource.LayersParameters vectorTileLayers,
    FlexConfig flexConfig,
    TraverseVisitor traverseVisitor
  ) {
    this.graph = graph;
    this.transitService = transitService;
    this.transitRoutingConfig = transitRoutingConfig;
    this.streetRoutingTimeout = streetRoutingTimeout;
    this.meterRegistry = meterRegistry;
    this.raptorConfig = raptorConfig;
    this.requestLogger = requestLogger;
    this.tileRendererManager = tileRendererManager;
    this.vectorTileLayers = vectorTileLayers;
    this.flexConfig = flexConfig;
    this.traverseVisitor = traverseVisitor;
    this.routeRequestDefaults = routeRequestDefaults;
  }

  /**
   * Create a server context valid for one http request only!
   */
  public static DefaultServerRequestContext create(
    TransitRoutingConfig transitRoutingConfig,
    RouteRequest routeRequestDefaults,
    Duration streetRoutingTimeout,
    RaptorConfig<TripSchedule> raptorConfig,
    Graph graph,
    TransitService transitService,
    MeterRegistry meterRegistry,
    VectorTilesResource.LayersParameters vectorTileLayers,
    FlexConfig flexConfig,
    @Nullable TraverseVisitor traverseVisitor,
    @Nullable String requestLogFile
  ) {
    return new DefaultServerRequestContext(
      graph,
      transitService,
      transitRoutingConfig,
      streetRoutingTimeout,
      routeRequestDefaults,
      meterRegistry,
      raptorConfig,
      RequestLoggerFactory.createLogger(requestLogFile),
      new TileRendererManager(graph, routeRequestDefaults.preferences()),
      vectorTileLayers,
      flexConfig,
      traverseVisitor
    );
  }

  @Override
  public RouteRequest defaultRouteRequest() {
    // Lazy initialize request-scoped request to avoid doing this when not needed
    if (routeRequest == null) {
      routeRequest = routeRequestDefaults.copyWithDateTimeNow();
    }
    return routeRequest;
  }

  /**
   * Return the default routing request locale(without cloning the request).
   */
  @Override
  public Locale defaultLocale() {
    return routeRequestDefaults.locale();
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
  public TransitTuningParameters transitTuningParameters() {
    return transitRoutingConfig;
  }

  @Override
  public RaptorTuningParameters raptorTuningParameters() {
    return transitRoutingConfig;
  }

  @Override
  public Duration streetRoutingTimeout() {
    return streetRoutingTimeout;
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

  @Override
  public FlexConfig flexConfig() {
    return flexConfig;
  }

  @Override
  public VectorTilesResource.LayersParameters vectorTileLayers() {
    return vectorTileLayers;
  }
}
