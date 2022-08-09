package org.opentripplanner.standalone.server;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.FileAppender;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.Locale;
import javax.annotation.Nullable;
import org.opentripplanner.inspector.TileRendererManager;
import org.opentripplanner.routing.RoutingService;
import org.opentripplanner.routing.algorithm.astar.TraverseVisitor;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.TripSchedule;
import org.opentripplanner.routing.api.request.RoutingRequest;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.standalone.api.OtpServerContext;
import org.opentripplanner.standalone.config.RouterConfig;
import org.opentripplanner.transit.raptor.configure.RaptorConfig;
import org.opentripplanner.transit.service.DefaultTransitService;
import org.opentripplanner.transit.service.TransitModel;
import org.opentripplanner.transit.service.TransitService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DefaultServerContext implements OtpServerContext {

  private RoutingRequest routingRequest = null;
  private final Graph graph;
  private final TransitModel transitModel;

  private TransitService transitService = null;
  private final RouterConfig routerConfig;
  private final MeterRegistry meterRegistry;
  private final RaptorConfig<TripSchedule> raptorConfig;
  public final Logger requestLogger;
  private final TileRendererManager tileRendererManager;
  public final TraverseVisitor traverseVisitor;

  /**
   * Copy constructor - used to make an HTTP Request scoped copy of the context. All mutable
   * components need to be coped here. This is
   */
  private DefaultServerContext(
    Graph graph,
    TransitModel transitModel,
    RouterConfig routerConfig,
    MeterRegistry meterRegistry,
    RaptorConfig<TripSchedule> raptorConfig,
    Logger requestLogger,
    TileRendererManager tileRendererManager,
    TraverseVisitor traverseVisitor
  ) {
    this.graph = graph;
    this.transitModel = transitModel;
    this.routerConfig = routerConfig;
    this.meterRegistry = meterRegistry;
    this.raptorConfig = raptorConfig;
    this.requestLogger = requestLogger;
    this.tileRendererManager = tileRendererManager;
    this.traverseVisitor = traverseVisitor;
  }

  /**
   * Create a default server context witch can be cloned by calling
   * {@link #createHttpRequestScopedCopy()} for each HTTP request.
   */
  public static DefaultServerContext create(
    RouterConfig routerConfig,
    RaptorConfig<TripSchedule> raptorConfig,
    Graph graph,
    TransitModel transitModel,
    MeterRegistry meterRegistry,
    @Nullable TraverseVisitor traverseVisitor
  ) {
    var defaultRoutingRequest = routerConfig.routingRequestDefaults();

    return new DefaultServerContext(
      graph,
      transitModel,
      routerConfig,
      meterRegistry,
      raptorConfig,
      createLogger(routerConfig.requestLogFile()),
      new TileRendererManager(graph, defaultRoutingRequest),
      traverseVisitor
    );
  }

  @Override
  public RoutingRequest defaultRoutingRequest() {
    // Lazy initialize request-scoped request to avoid doing this when not needed
    if (routingRequest == null) {
      routingRequest = routerConfig.routingRequestDefaults().copyWithDateTimeNow();
    }
    return routingRequest;
  }

  /**
   * Return the default routing request locale(without cloning the request).
   */
  @Override
  public Locale defaultLocale() {
    return routerConfig().routingRequestDefaults().locale;
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
    if (transitService == null) {
      this.transitService = new DefaultTransitService(transitModel);
    }
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

  public OtpServerContext createHttpRequestScopedCopy() {
    return new DefaultServerContext(
      graph,
      transitModel,
      routerConfig,
      meterRegistry,
      raptorConfig,
      requestLogger,
      tileRendererManager,
      traverseVisitor
    );
  }

  /**
   * Programmatically (i.e. not in XML) create a Logback logger for requests happening on this
   * router. http://stackoverflow.com/a/17215011/778449
   */
  private static Logger createLogger(@Nullable String file) {
    if (file == null) {
      return null;
    }

    LoggerContext lc = (LoggerContext) LoggerFactory.getILoggerFactory();
    PatternLayoutEncoder ple = new PatternLayoutEncoder();
    ple.setPattern("%d{yyyy-MM-dd'T'HH:mm:ss.SSS} %msg%n");
    ple.setContext(lc);
    ple.start();
    FileAppender<ILoggingEvent> fileAppender = new FileAppender<>();
    fileAppender.setFile(file);
    fileAppender.setEncoder(ple);
    fileAppender.setContext(lc);
    fileAppender.start();
    ch.qos.logback.classic.Logger logger = (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(
      "REQ_LOG"
    );
    logger.addAppender(fileAppender);
    logger.setLevel(Level.INFO);
    logger.setAdditive(false);
    return logger;
  }
}
