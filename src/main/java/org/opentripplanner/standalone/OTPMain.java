/* This program is free software: you can redistribute it and/or
 modify it under the terms of the GNU Lesser General Public License
 as published by the Free Software Foundation, either version 3 of
 the License, or (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>. */

package org.opentripplanner.standalone;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.MissingNode;
import com.google.common.collect.Lists;
import org.apache.bsf.BSFException;
import org.opentripplanner.analyst.request.IsoChroneSPTRendererAccSampling;
import org.opentripplanner.analyst.request.Renderer;
import org.opentripplanner.analyst.request.SampleGridRenderer;
import org.opentripplanner.analyst.request.TileCache;
import org.opentripplanner.graph_builder.AnnotationsToHTML;
import org.opentripplanner.graph_builder.GraphBuilder;
import org.opentripplanner.graph_builder.model.GtfsBundle;
import org.opentripplanner.graph_builder.module.*;
import org.opentripplanner.graph_builder.module.map.BusRouteStreetMatcher;
import org.opentripplanner.graph_builder.module.ned.ElevationModule;
import org.opentripplanner.graph_builder.module.ned.GeotiffGridCoverageFactoryImpl;
import org.opentripplanner.graph_builder.module.ned.NEDGridCoverageFactoryImpl;
import org.opentripplanner.graph_builder.module.osm.DefaultWayPropertySetSource;
import org.opentripplanner.graph_builder.module.osm.OpenStreetMapModule;
import org.opentripplanner.graph_builder.services.DefaultStreetEdgeFactory;
import org.opentripplanner.graph_builder.services.GraphBuilderModule;
import org.opentripplanner.graph_builder.services.ned.ElevationGridCoverageFactory;
import org.opentripplanner.inspector.TileRendererManager;
import org.opentripplanner.openstreetmap.impl.AnyFileBasedOpenStreetMapProviderImpl;
import org.opentripplanner.openstreetmap.services.OpenStreetMapProvider;
import org.opentripplanner.reflect.ReflectionLibrary;
import org.opentripplanner.reflect.ReflectiveInitializer;
import org.opentripplanner.routing.core.RoutingRequest;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.impl.*;
import org.opentripplanner.routing.services.GraphService;
import org.opentripplanner.scripting.impl.BSFOTPScript;
import org.opentripplanner.scripting.impl.OTPScript;
import org.opentripplanner.updater.GraphUpdaterConfigurator;
import org.opentripplanner.visualizer.GraphVisualizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.ParameterException;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * This is the main entry point to OpenTripPlanner. It allows both building graphs and starting up an OTP server
 * depending on command line options. OTPMain is a concrete class making it possible to construct one with custom
 * CommandLineParameters and use its graph builder construction method from web services or scripts, not just from the
 * static main function below.
 *
 * TODO still it seems fairly natural for all of these methods to be static.
 */
public class OTPMain {

    private static final Logger LOG = LoggerFactory.getLogger(OTPMain.class);

    public static final String BUILDER_CONFIG_FILENAME = "build-config.json";
    public static final String ROUTER_CONFIG_FILENAME = "router-config.json";
    public static final String OTP_CONFIG_FILENAME = "otp-config.json";

    private final CommandLineParameters params;
    public OTPServer otpServer = null;
    public GraphService graphService = null;

    /** ENTRY POINT: This is the main method that is called when running otp.jar from the command line. */
    public static void main(String[] args) {

        /* Parse and validate command line parameters. */
        CommandLineParameters params = new CommandLineParameters();
        try {
            JCommander jc = new JCommander(params, args);
            if (params.help) {
                jc.setProgramName("java -Xmx[several]G -jar otp.jar");
                jc.usage();
                System.exit(0);
            }
            params.infer();
        } catch (ParameterException pex) {
            LOG.error("Parameter error: {}", pex.getMessage());
            System.exit(1);
        }

        if (params.build == null && !params.visualize && !params.server && params.scriptFile == null) {
            LOG.info("Nothing to do. Use --help to see available tasks.");
            System.exit(-1);
        }

        OTPMain main = new OTPMain(params);
        main.run();

    }

    /* Constructor. */
    public OTPMain(CommandLineParameters params) {
        this.params = params;
    }

    /**
     * Making OTPMain a concrete class and placing this logic an instance method instead of embedding it in the static
     * main method makes it possible to build graphs from web services or scripts, not just from the command line.
     */
    public void run() {

        // TODO do params.infer() here to ensure coherency?

        /* Create the top-level objects that represent the OTP server. */
        makeGraphService(); // FIXME this triggers graph scanning/loading even when we are only building a graph -- perform that scanning separately
        otpServer = new OTPServer(params, graphService);

        /* Start graph builder if requested */
        if (params.build != null) {
            GraphBuilder graphBuilder = builderFromParameters();
            if (graphBuilder != null) {
                graphBuilder.run();
                /* If requested, hand off the graph to the server as the default graph using an in-memory GraphSource. */
                if (params.inMemory || params.preFlight) {
                    Graph graph = graphBuilder.getGraph();
                    graph.index(new DefaultStreetVertexIndexFactory());
                    // FIXME pass in Router JSON config
                    graphService.registerGraph("", new MemoryGraphSource("", graph, MissingNode.getInstance()));
                }
            } else {
                LOG.error("An error occurred while building the graph. Exiting.");
                System.exit(-1);
            }
        }

        /* Scan for graphs to load from disk if requested FIXME eventually router IDs will be present even when just building a graph. */
        if ((params.routerIds != null && params.routerIds.size() > 0) || params.autoScan) {
            /* Auto-register pre-existing graph on disk, with optional auto-scan. */
            GraphScanner graphScanner = new GraphScanner(graphService, params.graphDirectory, params.autoScan);
            graphScanner.basePath = params.graphDirectory;
            if (params.routerIds.size() > 0) {
                graphScanner.defaultRouterId = params.routerIds.get(0);
            }
            graphScanner.autoRegister = params.routerIds;
            graphScanner.startup();
        }

        /* Start visualizer if requested */
        if (params.visualize) {
            // FIXME get OTPServer into visualizer.
            GraphVisualizer visualizer = new GraphVisualizer(graphService.getRouter().graph);
            visualizer.run();
        }

        /* Start script if requested */
        if (params.scriptFile != null) {
            try {
                OTPScript otpScript = new BSFOTPScript(otpServer, params.scriptFile);
                if (otpScript != null) {
                    Object retval = otpScript.run();
                    if (retval != null) {
                        LOG.warn("Your script returned something, no idea what to do with it: {}", retval);
                    }
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        /* Start web server if requested */
        if (params.server) {
            GrizzlyServer grizzlyServer = new GrizzlyServer(params, otpServer);
            while (true) { // Loop to restart server on uncaught fatal exceptions.
                try {
                    grizzlyServer.run();
                    return;
                } catch (Throwable throwable) {
                    throwable.printStackTrace();
                    LOG.error("An uncaught {} occurred inside OTP. Restarting server.",
                            throwable.getClass().getSimpleName());
                }
            }
        }

    }

    /**
     * Create a cached GraphService that will be used by all OTP components to resolve router IDs to Graphs.
     * If a graph is supplied (graph parameter is not null) then that graph is also registered.
     * TODO move into OTPServer and/or GraphService
     */
    public void makeGraphService () {
        graphService = new GraphService(params.autoReload);
        InputStreamGraphSource.FileFactory graphSourceFactory = new InputStreamGraphSource.FileFactory(params.graphDirectory);
        graphService.graphSourceFactory = graphSourceFactory;
        graphService.routerLifecycleManager = routerLifecycleManager;
        if (params.graphDirectory != null) {
            graphSourceFactory.basePath = params.graphDirectory;
        }
    }

    // TODO parameterize with the router ID and call repeatedly to make multiple builders
    // note of all command line options this is only using  params.inMemory params.preFlight and params.build directory
    public GraphBuilder builderFromParameters() {
        LOG.info("Wiring up and configuring graph builder task.");
        GraphBuilder graphBuilder = new GraphBuilder();
        List<File> gtfsFiles = Lists.newArrayList();
        List<File> osmFiles =  Lists.newArrayList();
        JsonNode builderConfig = null;
        JsonNode routerConfig = null;
        File demFile = null;
        /* TODO build multiple graphs (previous implementation was broken and lumped together files from multiple directories) */
        File dir = params.build;
        LOG.info("Searching for graph builder input files in {}", dir);
        if ( ! dir.isDirectory() && dir.canRead()) {
            LOG.error("'{}' is not a readable directory.", dir);
            return null;
        }
        graphBuilder.setPath(dir);
        // Find and parse config files first to reveal syntax errors early without waiting for graph build.
        builderConfig = loadJson(new File(dir, BUILDER_CONFIG_FILENAME));
        GraphBuilderParameters builderParams = new GraphBuilderParameters(builderConfig);
        routerConfig = loadJson(new File(dir, ROUTER_CONFIG_FILENAME));
        // We have loaded the router config JSON but will actually apply it only when a router starts up
        LOG.info(ReflectionLibrary.dumpFields(builderParams));
        for (File file : dir.listFiles()) {
            switch (InputFileType.forFile(file)) {
                case GTFS:
                    LOG.info("Found GTFS file {}", file);
                    gtfsFiles.add(file);
                    break;
                case OSM:
                    LOG.info("Found OSM file {}", file);
                    osmFiles.add(file);
                    break;
                case DEM:
                    if (!builderParams.elevation && demFile == null) {
                        LOG.info("Found DEM file {}", file);
                        demFile = file;
                    } else {
                        LOG.info("Skipping DEM file {}", file);
                    }
                    break;
                case OTHER:
                    LOG.debug("Skipping file '{}'", file);
            }
        }
        boolean hasOSM  = builderParams.streets && !osmFiles.isEmpty();
        boolean hasGTFS = builderParams.transit && !gtfsFiles.isEmpty();
        if ( ! ( hasOSM || hasGTFS )) {
            LOG.error("Found no input files from which to build a graph in {}", params.build.toString());
            return null;
        }
        if ( hasOSM ) {
            List<OpenStreetMapProvider> osmProviders = Lists.newArrayList();
            for (File osmFile : osmFiles) {
                OpenStreetMapProvider osmProvider = new AnyFileBasedOpenStreetMapProviderImpl(osmFile);
                osmProviders.add(osmProvider);
            }
            OpenStreetMapModule osmBuilder = new OpenStreetMapModule(osmProviders);
            DefaultStreetEdgeFactory streetEdgeFactory = new DefaultStreetEdgeFactory();
            streetEdgeFactory.useElevationData = builderParams.elevation || (demFile != null);
            osmBuilder.edgeFactory = streetEdgeFactory;
            DefaultWayPropertySetSource defaultWayPropertySetSource = new DefaultWayPropertySetSource();
            osmBuilder.setDefaultWayPropertySetSource(defaultWayPropertySetSource);
            osmBuilder.skipVisibility = !builderParams.areaVisibility;
            graphBuilder.addGraphBuilder(osmBuilder);
            graphBuilder.addGraphBuilder(new PruneFloatingIslands());
        }
        if ( hasGTFS ) {
            List<GtfsBundle> gtfsBundles = Lists.newArrayList();
            for (File gtfsFile : gtfsFiles) {
                GtfsBundle gtfsBundle = new GtfsBundle(gtfsFile);
                gtfsBundle.setTransfersTxtDefinesStationPaths(builderParams.useTransfersTxt);
                if (builderParams.parentStopLinking) {
                    gtfsBundle.linkStopsToParentStations = true;
                }
                gtfsBundle.parentStationTransfers = builderParams.parentStationTransfers;
                gtfsBundles.add(gtfsBundle);
            }
            GtfsModule gtfsBuilder = new GtfsModule(gtfsBundles);
            graphBuilder.addGraphBuilder(gtfsBuilder);
            if ( hasOSM ) {
                if (builderParams.matchBusRoutesToStreets) {
                    graphBuilder.addGraphBuilder(new BusRouteStreetMatcher());
                }
                graphBuilder.addGraphBuilder(new TransitToTaggedStopsModule());
                graphBuilder.addGraphBuilder(new TransitToStreetNetworkModule());
            }
            // The stops can be linked to each other once they are already linked to the street network.
            if ( ! builderParams.useTransfersTxt) {
                // This module will use streets or straight line distance depending on whether OSM data is found in the graph.
                graphBuilder.addGraphBuilder(new DirectTransferGenerator());
            }
            gtfsBuilder.setFareServiceFactory(new DefaultFareServiceFactory());
        }
        if (builderParams.elevation) {
            File cacheDirectory = new File(params.cacheDirectory, "ned");
            ElevationGridCoverageFactory gcf = new NEDGridCoverageFactoryImpl(cacheDirectory);
            GraphBuilderModule elevationBuilder = new ElevationModule(gcf);
            graphBuilder.addGraphBuilder(elevationBuilder);
        } else  if (demFile != null) {
            ElevationGridCoverageFactory gcf = new GeotiffGridCoverageFactoryImpl(demFile);
            GraphBuilderModule elevationBuilder = new ElevationModule(gcf);
            graphBuilder.addGraphBuilder(elevationBuilder);
        }
        graphBuilder.addGraphBuilder(new EmbedConfig(builderConfig, routerConfig));
        if (builderParams.htmlAnnotations) {
            graphBuilder.addGraphBuilder(new AnnotationsToHTML(new File(params.build, "report.html")));
        }
        graphBuilder.serializeGraph = ( ! params.inMemory ) || params.preFlight;
        return graphBuilder;
    }

    /**
     * The default router lifecycle manager. Bind the services and delegates to
     * GraphUpdaterConfigurator the real-time updater startup/shutdown.
     */
    private static Router.LifecycleManager routerLifecycleManager = new Router.LifecycleManager() {

        /** Create a new Router, owning a Graph and all it's associated services. */
        @Override
        public void startupRouter(Router router, JsonNode config) {

            router.tileRendererManager = new TileRendererManager(router.graph);

            // Analyst Modules FIXME make these optional based on JSON?
            {
                router.tileCache = new TileCache(router.graph);
                router.renderer = new Renderer(router.tileCache);
                router.sampleGridRenderer = new SampleGridRenderer(router.graph);
                router.isoChroneSPTRenderer = new IsoChroneSPTRendererAccSampling(router.sampleGridRenderer);
            }

            /* Create the default router parameters from the JSON router config. */
            JsonNode routingDefaultsNode = config.get("routingDefaults");
            if (routingDefaultsNode != null) {
                LOG.info("Loading default routing parameters from JSON:");
                ReflectiveInitializer<RoutingRequest> scraper = new ReflectiveInitializer(RoutingRequest.class);
                router.defaultRoutingRequest = scraper.scrape(routingDefaultsNode);
            } else {
                LOG.info("No default routing parameters were found in the router config JSON. Using built-in OTP defaults.");
                router.defaultRoutingRequest = new RoutingRequest();
            }

            /* Apply single timeout. */
            JsonNode timeout = config.get("timeout");
            if (timeout != null) {
                if (timeout.isNumber()) {
                    router.timeouts = new double[]{timeout.doubleValue()};
                } else {
                    LOG.error("The 'timeout' configuration option should be a number of seconds.");
                }
            }

            /* Apply multiple timeouts. */
            JsonNode timeouts = config.get("timeouts");
            if (timeouts != null) {
                if (timeouts.isArray() && timeouts.size() > 0) {
                    router.timeouts = new double[timeouts.size()];
                    int i = 0;
                    for (JsonNode node : timeouts) {
                        router.timeouts[i++] = node.doubleValue();
                    }
                } else {
                    LOG.error("The 'timeouts' configuration option should be an array of values in seconds.");
                }
            }
            LOG.info("Timeouts for router '{}': {}", router.id, router.timeouts);

            /* Create Graph updater modules from JSON config. */
            GraphUpdaterConfigurator.setupGraph(router.graph, config);

        }

        @Override
        public void shutdownRouter(Router router) {
            GraphUpdaterConfigurator.shutdownGraph(router.graph);
        }
    };

    /**
     * Represents the different types of input files for a graph build.
     */
    private static enum InputFileType {
        GTFS, OSM, DEM, CONFIG, OTHER;
        public static InputFileType forFile(File file) {
            String name = file.getName();
            if (name.endsWith(".zip")) {
                try {
                    ZipFile zip = new ZipFile(file);
                    ZipEntry stopTimesEntry = zip.getEntry("stop_times.txt");
                    zip.close();
                    if (stopTimesEntry != null) return GTFS;
                } catch (Exception e) { /* fall through */ }
            }
            if (name.endsWith(".pbf")) return OSM;
            if (name.endsWith(".osm")) return OSM;
            if (name.endsWith(".osm.xml")) return OSM;
            if (name.endsWith(".tif")) return DEM;
            return OTHER;
        }
    }

    /**
     * Open and parse the JSON file at the given path into a Jackson JSON tree. Comments and unquoted keys are allowed.
     * Returns null if the file does not exist,
     * Returns null if the file contains syntax errors or cannot be parsed for some other reason.
     *
     * We do not require any JSON config files to be present because that would get in the way of the simplest
     * rapid deployment workflow. Therefore we return an empty JSON node when the file is missing, causing us to fall
     * back on all the default values as if there was a JSON file present with no fields defined.
     */
    private static JsonNode loadJson (File file) {
        try (FileInputStream jsonStream = new FileInputStream(file)) {
            ObjectMapper mapper = new ObjectMapper();
            mapper.configure(JsonParser.Feature.ALLOW_COMMENTS, true);
            mapper.configure(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES, true);
            JsonNode config = mapper.readTree(jsonStream);
            LOG.info("Found and loaded JSON configuration file '{}'", file);
            return config;
        } catch (FileNotFoundException ex) {
            LOG.info("File '{}' is not present. Using default configuration.", file);
            return MissingNode.getInstance();
        } catch (Exception ex) {
            LOG.error("Error while parsing JSON config file '{}': {}", file, ex.getMessage());
            System.exit(42); // probably "should" be done with an exception
            return null;
        }
    }


}
