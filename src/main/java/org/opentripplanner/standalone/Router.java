package org.opentripplanner.standalone;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.FileAppender;
import com.fasterxml.jackson.databind.JsonNode;
import org.opentripplanner.analyst.request.*;
import org.opentripplanner.analyst.scenario.ScenarioStore;
import org.opentripplanner.inspector.TileRendererManager;
import org.opentripplanner.reflect.ReflectiveInitializer;
import org.opentripplanner.routing.core.RoutingRequest;
import org.opentripplanner.routing.core.TraverseMode;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.updater.GraphUpdaterConfigurator;
import org.opentripplanner.visualizer.GraphVisualizer;
import org.slf4j.LoggerFactory;

import java.util.EnumMap;

/**
 * Represents the configuration of a single router (a single graph for a specific geographic area)
 * in an OTP server.
 */
public class Router {

    private static final Logger LOG = (Logger) LoggerFactory.getLogger(Router.class);

    public static final String ROUTER_CONFIG_FILENAME = "router-config.json";

    public String id;
    public Graph graph;
    public double[] timeouts = {5, 4, 2};

    /**
     *  Separate logger for incoming requests. This should be handled with a Logback logger rather than something
     *  simple like a PrintStream because requests come in multi-threaded.
     */
    public Logger requestLogger = null;

    /* TODO The fields for "components" are slowly disappearing... maybe at some point a router will be nothing but configuration values tied to a Graph. */

    // Inspector/debug services
    public TileRendererManager tileRendererManager;

    // Analyst services
    public TileCache tileCache;
    public Renderer renderer;
    public IsoChroneSPTRenderer isoChroneSPTRenderer;
    public SampleGridRenderer sampleGridRenderer;

    // A RoutingRequest containing default parameters that will be cloned when handling each request
    public RoutingRequest defaultRoutingRequest;

    /** A graphical window that is used for visualizing search progress (debugging). */
    public GraphVisualizer graphVisualizer = null;

    /** Storage for non-destructive alternatives analysis scenarios. */
    public ScenarioStore scenarioStore = new ScenarioStore();

    public Router(String id, Graph graph) {
        this.id = id;
        this.graph = graph;
    }


    /**
     * Below is functionality moved into Router from the "router lifecycle manager" interface and implementation.
     * Current responsibilities are: 1) Binding proper services (depending on the configuration from command-line or
     * JSON config files) and 2) starting / stopping real-time updaters (delegated to the GraphUpdaterConfigurator class).
     */

    /**
     * Start up a new router once it has been created.
     * @param config The configuration (loaded from Graph.properties for example).
     */
    public void startup(JsonNode config) {

        this.tileRendererManager = new TileRendererManager(this.graph);

        // Analyst Modules FIXME make these optional based on JSON?
        {
            this.tileCache = new TileCache(this.graph);
            this.renderer = new Renderer(this.tileCache);
            this.sampleGridRenderer = new SampleGridRenderer(this.graph);
            this.isoChroneSPTRenderer = new IsoChroneSPTRendererAccSampling(this.sampleGridRenderer);
        }

        /* Create the default router parameters from the JSON router config. */
        JsonNode routingDefaultsNode = config.get("routingDefaults");
        if (routingDefaultsNode != null) {
            LOG.info("Loading default routing parameters from JSON:");
            ReflectiveInitializer<RoutingRequest> scraper = new ReflectiveInitializer(RoutingRequest.class);
            this.defaultRoutingRequest = scraper.scrape(routingDefaultsNode);
        } else {
            LOG.info("No default routing parameters were found in the router config JSON. Using built-in OTP defaults.");
            this.defaultRoutingRequest = new RoutingRequest();
        }

        /* Apply single timeout. */
        JsonNode timeout = config.get("timeout");
        if (timeout != null) {
            if (timeout.isNumber()) {
                this.timeouts = new double[]{timeout.doubleValue()};
            } else {
                LOG.error("The 'timeout' configuration option should be a number of seconds.");
            }
        }

        /* Apply multiple timeouts. */
        JsonNode timeouts = config.get("timeouts");
        if (timeouts != null) {
            if (timeouts.isArray() && timeouts.size() > 0) {
                this.timeouts = new double[timeouts.size()];
                int i = 0;
                for (JsonNode node : timeouts) {
                    this.timeouts[i++] = node.doubleValue();
                }
            } else {
                LOG.error("The 'timeouts' configuration option should be an array of values in seconds.");
            }
        }
        LOG.info("Timeouts for router '{}': {}", this.id, this.timeouts);

        JsonNode requestLogFile = config.get("requestLogFile");
        if (requestLogFile != null) {
            this.requestLogger = createLogger(requestLogFile.asText());
            LOG.info("Logging incoming requests at '{}'", requestLogFile.asText());
        } else {
            LOG.info("Incoming requests will not be logged.");
        }

        JsonNode boardTimes = config.get("boardTimes");
        if (boardTimes != null && boardTimes.isObject()) {
            graph.boardTimes = new EnumMap<>(TraverseMode.class);
            for (TraverseMode mode : TraverseMode.values()) {
                if (boardTimes.has(mode.name())) {
                    graph.boardTimes.put(mode, boardTimes.get(mode.name()).asInt(0));
                }
            }
        }

        JsonNode alightTimes = config.get("alightTimes");
        if (alightTimes != null && alightTimes.isObject()) {
            graph.alightTimes = new EnumMap<>(TraverseMode.class);
            for (TraverseMode mode : TraverseMode.values()) {
                if (alightTimes.has(mode.name())) {
                    graph.alightTimes.put(mode, alightTimes.get(mode.name()).asInt(0));
                }
            }
        }

        /* Create Graph updater modules from JSON config. */
        GraphUpdaterConfigurator.setupGraph(this.graph, config);

    }

    /** Shut down this router when evicted or (auto-)reloaded. Stop any real-time updater threads. */
    public void shutdown() {
        GraphUpdaterConfigurator.shutdownGraph(this.graph);
    }

    /**
     * Programmatically (i.e. not in XML) create a Logback logger for requests happening on this router.
     * http://stackoverflow.com/a/17215011/778449
     */
    private static Logger createLogger(String file) {
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
        Logger logger = (Logger) LoggerFactory.getLogger("REQ_LOG");
        logger.addAppender(fileAppender);
        logger.setLevel(Level.INFO);
        logger.setAdditive(false);
        return logger;
    }

}
