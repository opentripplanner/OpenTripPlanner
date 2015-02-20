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

import java.io.*;
import java.lang.reflect.Field;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.MissingNode;
import org.apache.bsf.BSFException;
import org.opentripplanner.analyst.request.IsoChroneSPTRendererAccSampling;
import org.opentripplanner.analyst.request.Renderer;
import org.opentripplanner.analyst.request.SPTCache;
import org.opentripplanner.analyst.request.SampleGridRenderer;
import org.opentripplanner.analyst.request.TileCache;
import org.opentripplanner.api.resource.PlanGenerator;
import org.opentripplanner.graph_builder.AnnotationsToHTML;
import org.opentripplanner.graph_builder.GraphBuilder;
import org.opentripplanner.graph_builder.module.*;
import org.opentripplanner.graph_builder.module.ned.ElevationModule;
import org.opentripplanner.graph_builder.module.ned.GeotiffGridCoverageFactoryImpl;
import org.opentripplanner.graph_builder.module.ned.NEDGridCoverageFactoryImpl;
import org.opentripplanner.graph_builder.module.osm.DefaultWayPropertySetSource;
import org.opentripplanner.graph_builder.module.osm.OpenStreetMapModule;
import org.opentripplanner.graph_builder.model.GtfsBundle;
import org.opentripplanner.graph_builder.services.DefaultStreetEdgeFactory;
import org.opentripplanner.graph_builder.services.GraphBuilderModule;
import org.opentripplanner.graph_builder.services.ned.ElevationGridCoverageFactory;
import org.opentripplanner.inspector.TileRendererManager;
import org.opentripplanner.openstreetmap.impl.AnyFileBasedOpenStreetMapProviderImpl;
import org.opentripplanner.openstreetmap.services.OpenStreetMapProvider;
import org.opentripplanner.routing.core.RoutingRequest;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.impl.DefaultFareServiceFactory;
import org.opentripplanner.routing.impl.GenericAStarFactory;
import org.opentripplanner.routing.impl.GraphScanner;
import org.opentripplanner.routing.impl.InputStreamGraphSource;
import org.opentripplanner.routing.impl.LongDistancePathService;
import org.opentripplanner.routing.impl.MemoryGraphSource;
import org.opentripplanner.routing.services.GraphService;
import org.opentripplanner.servlet.ReflectiveQueryScraper;
import org.opentripplanner.updater.GraphUpdaterConfigurator;
import org.opentripplanner.scripting.impl.BSFOTPScript;
import org.opentripplanner.scripting.impl.OTPScript;
import org.opentripplanner.visualizer.GraphVisualizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;
import org.opentripplanner.graph_builder.module.map.BusRouteStreetMatcher;

public class OTPConfigurator {

    private static final Logger LOG = LoggerFactory.getLogger(OTPConfigurator.class);
    
    private final CommandLineParameters params;
    
    private GraphService graphService = null;
    
    public OTPConfigurator (CommandLineParameters params) {
        this.params = params;
    }

    private OTPServer server;

    public static final String BUILDER_CONFIG_FILENAME = "build-config.json";
    public static final String ROUTER_CONFIG_FILENAME = "router-config.json";

    /**
     * We could even do this at Configurator construct time (rather than lazy initializing), using
     * the inMemory param to create the right kind of GraphService ahead of time. However that
     * would create indexes even when only a build was going to happen.
     */
    public OTPServer getServer() {
        if (server == null) {
            server = new OTPServer(params, getGraphService());
        }
        return server;
    }

    /** Create a cached GraphService that will be shared between all OTP components. */
    public void makeGraphService (Graph graph) {
        GraphService graphService = new GraphService(params.autoReload);
        this.graphService = graphService;
        InputStreamGraphSource.FileFactory graphSourceFactory = new InputStreamGraphSource.FileFactory(params.graphDirectory);
        graphService.graphSourceFactory = graphSourceFactory;
        graphService.routerLifecycleManager = routerLifecycleManager;
        if (params.graphDirectory != null) {
            graphSourceFactory.basePath = params.graphDirectory;
        }
        if (graph != null && (params.inMemory || params.preFlight)) {
            /* Hand off graph in memory to server in a in-memory GraphSource. */
            // FIXME pass in Router config
            this.graphService.registerGraph("", new MemoryGraphSource("", graph, MissingNode.getInstance()));
        }
        if ((params.routerIds != null && params.routerIds.size() > 0) || params.autoScan) {
            /* Auto-register pre-existing graph on disk, with optional auto-scan. */
            GraphScanner graphScanner = new GraphScanner(graphService, params.graphDirectory, params.autoScan);
            graphScanner.basePath = graphSourceFactory.basePath;
            if (params.routerIds.size() > 0) {
                graphScanner.defaultRouterId = params.routerIds.get(0);
            }
            graphScanner.autoRegister = params.routerIds;
            graphScanner.startup();
        }
    }

    /** Return the cached, shared GraphService, making one as needed. */
    public GraphService getGraphService () {
        if (graphService == null) {
            makeGraphService(null);
        }
        return graphService;
    }

    // TODO parameterize with the build directory, to make multiple builderTasks
    public GraphBuilder builderFromParameters() {
        if (params.build == null) {
            return null;
        }
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
        LOG.info(dumpFields(builderParams));
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

    public GrizzlyServer serverFromParameters() {
        if (params.server) {
            GrizzlyServer server = new GrizzlyServer(params, getServer());
            return server;
        } else return null;
    }
    
    public GraphVisualizer visualizerFromParameters() {
        if (params.visualize) {
            // FIXME get OTPServer into visualizer.
            getServer();
            GraphVisualizer visualizer = new GraphVisualizer(getGraphService().getRouter().graph);
            return visualizer;
        } else return null;
    }
    
    /**
     * The default router lifecycle manager. Bind the services and delegates to
     * GraphUpdaterConfigurator the real-time updater startup/shutdown.
     */
    private Router.LifecycleManager routerLifecycleManager = new Router.LifecycleManager() {

        /** Create a new Router, owning a Graph and all it's associated services. */
        @Override
        public void startupRouter(Router router, JsonNode config) {

            router.sptServiceFactory = new GenericAStarFactory();
            LongDistancePathService pathService = new LongDistancePathService(router.graph, router.sptServiceFactory);
            router.pathService = pathService;
            router.planGenerator = new PlanGenerator(router.graph, router.pathService);
            router.tileRendererManager = new TileRendererManager(router.graph);

            // Optional Analyst Modules.
            if (params.analyst) {
                router.tileCache = new TileCache(router.graph);
                router.sptCache = new SPTCache(router.sptServiceFactory, router.graph);
                router.renderer = new Renderer(router.tileCache, router.sptCache);
                router.sampleGridRenderer = new SampleGridRenderer(router.graph,
                        router.sptServiceFactory);
                router.isoChroneSPTRenderer = new IsoChroneSPTRendererAccSampling(
                        router.sampleGridRenderer);
            }

            /* Create the default router parameters from the JSON router config. */
            ReflectiveQueryScraper<RoutingRequest> scraper = new ReflectiveQueryScraper(RoutingRequest.class);
            JsonNode routingDefaultsNode = config.get("routingDefaults");
            if (routingDefaultsNode != null) {
                LOG.info("Loading default routing parameters from JSON:");
                router.defaultRoutingRequest = scraper.scrape(routingDefaultsNode);
            } else {
                LOG.info("No default routing parameters were found in the router config JSON. Using built-in OTP defaults.");
            }

            /* Create Graph updater modules from JSON config. */
            GraphUpdaterConfigurator.setupGraph(router.graph, config);

        }

        @Override
        public void shutdownRouter(Router router) {
            GraphUpdaterConfigurator.shutdownGraph(router.graph);
        }
    };

    public OTPScript scriptFromParameters() throws BSFException, IOException {
        if (params.scriptFile != null) {
            return new BSFOTPScript(getServer(), params.scriptFile);
        } else
            return null;
    }

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

    /** Concatenate all fields and values of a Java object. */
    public static String dumpFields (Object object) {
        StringBuilder sb = new StringBuilder();
        Class clazz = object.getClass();
        sb.append("Summarizing ");
        sb.append(clazz.getSimpleName());
        sb.append('\n');
        for (Field field : clazz.getFields()) {
            sb.append(field.getName());
            sb.append(" = ");
            try {
                String value = field.get(object).toString();
                sb.append(value);
            } catch (IllegalAccessException ex) {
                sb.append("(non-public)");
            }
            sb.append('\n');
        }
        return sb.toString();
    }

}
