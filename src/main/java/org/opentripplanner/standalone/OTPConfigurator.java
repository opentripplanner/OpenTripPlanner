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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;
import java.util.prefs.Preferences;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.apache.bsf.BSFException;
import org.opentripplanner.analyst.request.IsoChroneSPTRendererAccSampling;
import org.opentripplanner.analyst.request.Renderer;
import org.opentripplanner.analyst.request.SPTCache;
import org.opentripplanner.analyst.request.SampleGridRenderer;
import org.opentripplanner.analyst.request.TileCache;
import org.opentripplanner.api.resource.PlanGenerator;
import org.opentripplanner.graph_builder.GraphBuilderTask;
import org.opentripplanner.graph_builder.impl.EmbeddedConfigGraphBuilderImpl;
import org.opentripplanner.graph_builder.impl.GtfsGraphBuilderImpl;
import org.opentripplanner.graph_builder.impl.PruneFloatingIslands;
import org.opentripplanner.graph_builder.impl.DirectTransferGenerator;
import org.opentripplanner.graph_builder.impl.TransitToStreetNetworkGraphBuilderImpl;
import org.opentripplanner.graph_builder.impl.TransitToTaggedStopsGraphBuilderImpl;
import org.opentripplanner.graph_builder.impl.ned.ElevationGraphBuilderImpl;
import org.opentripplanner.graph_builder.impl.ned.GeotiffGridCoverageFactoryImpl;
import org.opentripplanner.graph_builder.impl.ned.NEDGridCoverageFactoryImpl;
import org.opentripplanner.graph_builder.impl.osm.DefaultWayPropertySetSource;
import org.opentripplanner.graph_builder.impl.osm.OpenStreetMapGraphBuilderImpl;
import org.opentripplanner.graph_builder.model.GtfsBundle;
import org.opentripplanner.graph_builder.services.DefaultStreetEdgeFactory;
import org.opentripplanner.graph_builder.services.GraphBuilder;
import org.opentripplanner.graph_builder.services.ned.ElevationGridCoverageFactory;
import org.opentripplanner.inspector.TileRendererManager;
import org.opentripplanner.openstreetmap.impl.AnyFileBasedOpenStreetMapProviderImpl;
import org.opentripplanner.openstreetmap.services.OpenStreetMapProvider;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.impl.DefaultFareServiceFactory;
import org.opentripplanner.routing.impl.GenericAStarFactory;
import org.opentripplanner.routing.impl.GraphScanner;
import org.opentripplanner.routing.impl.InputStreamGraphSource;
import org.opentripplanner.routing.impl.LongDistancePathService;
import org.opentripplanner.routing.impl.MemoryGraphSource;
import org.opentripplanner.routing.impl.RetryingPathServiceImpl;
import org.opentripplanner.routing.services.GraphService;
import org.opentripplanner.updater.GraphUpdaterConfigurator;
import org.opentripplanner.scripting.impl.BSFOTPScript;
import org.opentripplanner.scripting.impl.OTPScript;
import org.opentripplanner.updater.PropertiesPreferences;
import org.opentripplanner.visualizer.GraphVisualizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;
import org.opentripplanner.graph_builder.impl.map.BusRouteStreetMatcher;

public class OTPConfigurator {

    private static final Logger LOG = LoggerFactory.getLogger(OTPConfigurator.class);
    
    private final CommandLineParameters params;
    
    private GraphService graphService = null;
    
    public OTPConfigurator (CommandLineParameters params) {
        this.params = params;
    }

    private OTPServer server;
    
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
    public void makeGraphService(Graph graph) {
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
            try {
                FileInputStream graphConfiguration = new FileInputStream(params.graphConfigFile);
                Preferences config = new PropertiesPreferences(graphConfiguration);
                this.graphService.registerGraph("", new MemoryGraphSource("", graph, config));
            } catch (Exception e) {
                if (params.graphConfigFile != null)
                    LOG.error("Can't read config file", e);
                this.graphService.registerGraph("", new MemoryGraphSource("", graph));
            }
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
    
    public GraphBuilderTask builderFromParameters() {
        if (params.build == null || params.build.isEmpty()) {
            return null;
        }
        LOG.info("Wiring up and configuring graph builder task.");
        GraphBuilderTask graphBuilder = new GraphBuilderTask();
        List<File> gtfsFiles = Lists.newArrayList();
        List<File> osmFiles =  Lists.newArrayList();
        File configFile = null;
        File demFile = null;
        /* For now this is adding files from all directories listed, rather than building multiple graphs. */
        for (File dir : params.build) {
            LOG.info("Searching for graph builder input files in {}", dir);
            if ( ! dir.isDirectory() && dir.canRead()) {
                LOG.error("'{}' is not a readable directory.", dir);
                continue;
            }
            graphBuilder.setPath(dir);
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
                    if (!params.elevation && demFile == null) {
                        LOG.info("Found DEM file {}", file);
                        demFile = file;
                    } else {
                        LOG.info("Skipping DEM file {}", file);
                    }
                    break;
                case CONFIG:
                    if (!params.noEmbedConfig) {
                        LOG.info("Found CONFIG file {}", file);
                        configFile = file;
                    }
                    break;
                case OTHER:
                    LOG.debug("Skipping file '{}'", file);
                }
            }
        }
        boolean hasOSM  = ! (osmFiles.isEmpty()  || params.noStreets);
        boolean hasGTFS = ! (gtfsFiles.isEmpty() || params.noTransit);
        if ( ! (hasOSM || hasGTFS )) {
            LOG.error("Found no input files from which to build a graph in {}", params.build.toString());
            return null;
        }
        if ( hasOSM ) {
            List<OpenStreetMapProvider> osmProviders = Lists.newArrayList();
            for (File osmFile : osmFiles) {
                OpenStreetMapProvider osmProvider = new AnyFileBasedOpenStreetMapProviderImpl(osmFile);
                osmProviders.add(osmProvider);
            }
            OpenStreetMapGraphBuilderImpl osmBuilder = new OpenStreetMapGraphBuilderImpl(osmProviders);
            DefaultStreetEdgeFactory streetEdgeFactory = new DefaultStreetEdgeFactory();
            streetEdgeFactory.useElevationData = params.elevation || (demFile != null);
            osmBuilder.edgeFactory = streetEdgeFactory;
            DefaultWayPropertySetSource defaultWayPropertySetSource = new DefaultWayPropertySetSource();
            osmBuilder.setDefaultWayPropertySetSource(defaultWayPropertySetSource);
            osmBuilder.skipVisibility = params.skipVisibility;
            graphBuilder.addGraphBuilder(osmBuilder);
            graphBuilder.addGraphBuilder(new PruneFloatingIslands());
        }
        if ( hasGTFS ) {
            List<GtfsBundle> gtfsBundles = Lists.newArrayList();
            for (File gtfsFile : gtfsFiles) {
                GtfsBundle gtfsBundle = new GtfsBundle(gtfsFile);
                gtfsBundle.setTransfersTxtDefinesStationPaths(params.useTransfersTxt);
                if (!params.noParentStopLinking) {
                    gtfsBundle.linkStopsToParentStations = true;
                }
                gtfsBundle.parentStationTransfers = params.parentStationTransfers;
                gtfsBundles.add(gtfsBundle);
            }
            GtfsGraphBuilderImpl gtfsBuilder = new GtfsGraphBuilderImpl(gtfsBundles);
            graphBuilder.addGraphBuilder(gtfsBuilder);
            if ( hasOSM ) {
                if (params.matchBusRoutesToStreets) {
                    graphBuilder.addGraphBuilder(new BusRouteStreetMatcher());
                }
                graphBuilder.addGraphBuilder(new TransitToTaggedStopsGraphBuilderImpl());
                graphBuilder.addGraphBuilder(new TransitToStreetNetworkGraphBuilderImpl());
            }
            // The stops can be linked to each other once they are already linked to the street network.
            if (params.longDistance && !params.useTransfersTxt) {
                // This module will use streets or straight line distance depending on whether OSM data is found in the graph.
                graphBuilder.addGraphBuilder(new DirectTransferGenerator());
            }
            gtfsBuilder.setFareServiceFactory(new DefaultFareServiceFactory());
        }
        if (configFile != null) {
            EmbeddedConfigGraphBuilderImpl embeddedConfigBuilder = new EmbeddedConfigGraphBuilderImpl();
            embeddedConfigBuilder.propertiesFile = configFile;
            graphBuilder.addGraphBuilder(embeddedConfigBuilder);
        }
        if (params.elevation) {
            File cacheDirectory = new File(params.cacheDirectory, "ned");
            ElevationGridCoverageFactory gcf = new NEDGridCoverageFactoryImpl(cacheDirectory);
            GraphBuilder elevationBuilder = new ElevationGraphBuilderImpl(gcf);
            graphBuilder.addGraphBuilder(elevationBuilder);
        } else  if (demFile != null) {
            ElevationGridCoverageFactory gcf = new GeotiffGridCoverageFactoryImpl(demFile);
            GraphBuilder elevationBuilder = new ElevationGraphBuilderImpl(gcf);
            graphBuilder.addGraphBuilder(elevationBuilder);
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

        private GraphUpdaterConfigurator graphConfigurator = new GraphUpdaterConfigurator();

        /**
         * Create a new Router, owning a Graph and all it's associated services.
         * 
         * TODO: We could parametrize some services based on the given graph "preferences" (ie
         * Graph.properties) instead of the command-line parameters. That would help simplify the
         * growing list of OTP command-line parameters and allow for different configuration based
         * on the routers (for example picking different path services for each routers, or enabling
         * analyst for some routers only).
         */
        @Override
        public void startupRouter(Router router, Preferences config) {

            router.sptServiceFactory = new GenericAStarFactory();
            // Choose a PathService to wrap the SPTService, depending on expected maximum path lengths
            if (params.longDistance) {
                LongDistancePathService pathService = new LongDistancePathService(router.graph,
                        router.sptServiceFactory);
                router.pathService = pathService;
            } else {
                RetryingPathServiceImpl pathService = new RetryingPathServiceImpl(router.graph,
                        router.sptServiceFactory);
                pathService.setFirstPathTimeout(10.0);
                pathService.setMultiPathTimeout(1.0);
                router.pathService = pathService;
                // cpf.bind(RemainingWeightHeuristicFactory.class,
                //        new DefaultRemainingWeightHeuristicFactoryImpl());
            }
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

            // Setup graph from config (Graph.properties for example)
            graphConfigurator.setupGraph(router.graph, config);
        }

        @Override
        public void shutdownRouter(Router router) {
            graphConfigurator.shutdownGraph(router.graph);
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
            if (name.equals("Embed.properties")) return CONFIG;
            return OTHER;
        }
    }
}
