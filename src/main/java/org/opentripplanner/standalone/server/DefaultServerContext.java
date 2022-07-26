package org.opentripplanner.standalone.server;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.FileAppender;
import io.micrometer.core.instrument.MeterRegistry;
import java.time.Instant;
import java.util.Locale;
import javax.annotation.Nullable;
import org.opentripplanner.inspector.TileRendererManager;
import org.opentripplanner.routing.RoutingService;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.TripSchedule;
import org.opentripplanner.routing.api.request.RoutingRequest;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.standalone.api.OtpServerContext;
import org.opentripplanner.standalone.config.RouterConfig;
import org.opentripplanner.transit.raptor.configure.RaptorConfig;
import org.opentripplanner.transit.service.DefaultTransitService;
import org.opentripplanner.transit.service.TransitModel;
import org.opentripplanner.transit.service.TransitService;
import org.opentripplanner.visualizer.GraphVisualizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DefaultServerContext implements OtpServerContext {

  private final RoutingRequest defaultRoutingRequest;
  private final Graph graph;
  private final TransitModel transitModel;
  private final RouterConfig routerConfig;
  private final MeterRegistry meterRegistry;
  private final RaptorConfig<TripSchedule> raptorConfig;
  public final Logger requestLogger;
  private final TileRendererManager tileRendererManager;
  public final GraphVisualizer graphVisualizer;

  public DefaultServerContext(
    Graph graph,
    TransitModel transitModel,
    RouterConfig routerConfig,
    MeterRegistry meterRegistry,
    boolean initGraphVisualizer
  ) {
    this.graph = graph;
    this.transitModel = transitModel;
    this.routerConfig = routerConfig;
    this.defaultRoutingRequest = routerConfig.routingRequestDefaults();
    this.meterRegistry = meterRegistry;
    this.raptorConfig = new RaptorConfig<>(routerConfig.raptorTuningParameters());
    this.requestLogger = createLogger(routerConfig.requestLogFile());
    this.tileRendererManager = new TileRendererManager(this.graph, this.defaultRoutingRequest);
    this.graphVisualizer = initGraphVisualizer ? new GraphVisualizer(this) : null;
  }

  /**
   * A RoutingRequest containing default parameters that will be cloned when handling each request.
   */
  @Override
  public RoutingRequest copyDefaultRoutingRequest() {
    var copy = this.defaultRoutingRequest.clone();
    copy.setDateTime(Instant.now());
    return copy;
  }

  /**
   * Return the default routing request locale(without cloning the request).
   */
  @Override
  public Locale getDefaultLocale() {
    return this.defaultRoutingRequest.locale;
  }

  @Override
  public double streetRoutingTimeoutSeconds() {
    return routerConfig.streetRoutingTimeoutSeconds();
  }

  @Override
  public Graph graph() {
    return graph;
  }

  @Override
  public TransitModel transitModel() {
    return transitModel;
  }

  @Override
  public RouterConfig routerConfig() {
    return routerConfig;
  }

  @Override
  public MeterRegistry meterRegistry() {
    return meterRegistry;
  }

  @Override
  public RaptorConfig<TripSchedule> raptorConfig() {
    return raptorConfig;
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
  public GraphVisualizer graphVisualizer() {
    return graphVisualizer;
  }

  @Override
  public RoutingService routingRequestService() {
    return new RoutingService(graph(), transitModel());
  }

  @Override
  public TransitService transitRequestService() {
    return new DefaultTransitService(transitModel());
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
