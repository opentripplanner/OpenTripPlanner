package org.opentripplanner.standalone.server;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.FileAppender;
import org.opentripplanner.inspector.TileRendererManager;
import org.opentripplanner.routing.algorithm.raptor.transit.TransitLayer;
import org.opentripplanner.routing.algorithm.raptor.transit.TripSchedule;
import org.opentripplanner.routing.algorithm.raptor.transit.mappers.TransitLayerMapper;
import org.opentripplanner.routing.algorithm.raptor.transit.mappers.TransitLayerUpdater;
import org.opentripplanner.routing.api.request.RoutingRequest;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.standalone.config.RouterConfig;
import org.opentripplanner.transit.raptor.rangeraptor.configure.RaptorConfig;
import org.opentripplanner.updater.GraphUpdaterConfigurator;
import org.opentripplanner.util.ElevationUtils;
import org.opentripplanner.util.WorldEnvelope;
import org.opentripplanner.visualizer.GraphVisualizer;
import org.slf4j.LoggerFactory;

/**
 * Represents the configuration of a single router (a single graph for a specific geographic area)
 * in an OTP server.
 */
public class Router {

    private static final org.slf4j.Logger LOG = LoggerFactory.getLogger(Router.class);
    public final Graph graph;
    public final RouterConfig routerConfig;
    public final RaptorConfig<TripSchedule> raptorConfig;

    /**
     *  Separate logger for incoming requests. This should be handled with a Logback logger
     *  rather than something simple like a PrintStream because requests come in multi-threaded.
     */
    public ch.qos.logback.classic.Logger requestLogger = null;

    /* TODO The fields for "components" are slowly disappearing... maybe at some point a router
        will be nothing but configuration values tied to a Graph. */

    /** Inspector/debug services */
    public TileRendererManager tileRendererManager;

    /**
     * A RoutingRequest containing default parameters that will be cloned when handling each
     * request.
     */
    public RoutingRequest defaultRoutingRequest;

    /** A graphical window that is used for visualizing search progress (debugging). */
    public GraphVisualizer graphVisualizer = null;

    public Router(Graph graph, RouterConfig routerConfig) {
        this.graph = graph;
        this.routerConfig = routerConfig;
        this.raptorConfig = new RaptorConfig<>(routerConfig.raptorTuningParameters());
    }

    /*
     * Below is functionality moved into Router from the "router lifecycle manager" interface and implementation.
     * Current responsibilities are: 1) Binding proper services (depending on the configuration from command-line or
     * JSON config files) and 2) starting / stopping real-time updaters (delegated to the GraphUpdaterConfigurator class).
     */

    /**
     * Start up a new router once it has been created.
     */
    public void startup() {
        this.tileRendererManager = new TileRendererManager(this.graph);
        this.defaultRoutingRequest = routerConfig.routingRequestDefaults();

        if (routerConfig.requestLogFile() != null) {
            this.requestLogger = createLogger(routerConfig.requestLogFile());
            LOG.info("Logging incoming requests at '{}'", routerConfig.requestLogFile());
        } else {
            LOG.info("Incoming requests will not be logged.");
        }

        /* Create transit layer for Raptor routing. Here we map the scheduled timetables. */
        /* Realtime updates can be mapped similarly by a recurring operation in a GraphUpdater below. */
        LOG.info("Creating transit layer for Raptor routing.");
        if (graph.hasTransit && graph.index != null) {
            graph.setTransitLayer(TransitLayerMapper.map(routerConfig.transitTuningParameters(), graph));
            graph.setRealtimeTransitLayer(new TransitLayer(graph.getTransitLayer()));
            graph.transitLayerUpdater = new TransitLayerUpdater(
                graph,
                graph.index.getServiceCodesRunningForDate()
            );
        } else {
            LOG.warn("Cannot create Raptor data, that requires the graph to have transit data and be indexed.");
        }

        /* Create Graph updater modules from JSON config. */
        GraphUpdaterConfigurator.setupGraph(this.graph, routerConfig.rawJson);

        /* Compute ellipsoidToGeoidDifference for this Graph */
        try {
            WorldEnvelope env = graph.getEnvelope();
            double lat = (env.getLowerLeftLatitude() + env.getUpperRightLatitude()) / 2;
            double lon = (env.getLowerLeftLongitude() + env.getUpperRightLongitude()) / 2;
            graph.ellipsoidToGeoidDifference = ElevationUtils.computeEllipsoidToGeoidDifference(lat, lon);
            LOG.info("Computed ellipsoid/geoid offset at (" + lat + ", " + lon + ") as " + graph.ellipsoidToGeoidDifference);
        } catch (Exception e) {
            LOG.error("Error computing ellipsoid/geoid difference");
        }
    }

    /** Shut down this router when evicted or (auto-)reloaded. Stop any real-time updater threads. */
    public void shutdown() {
        GraphUpdaterConfigurator.shutdownGraph(this.graph);
    }

    /**
     * Programmatically (i.e. not in XML) create a Logback logger for requests happening on this router.
     * http://stackoverflow.com/a/17215011/778449
     */
    private static ch.qos.logback.classic.Logger createLogger(String file) {
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
        ch.qos.logback.classic.Logger logger = (ch.qos.logback.classic.Logger) LoggerFactory.getLogger("REQ_LOG");
        logger.addAppender(fileAppender);
        logger.setLevel(Level.INFO);
        logger.setAdditive(false);
        return logger;
    }

    public double streetRoutingTimeoutSeconds() {
        return  routerConfig.streetRoutingTimeoutSeconds();
    }
}
