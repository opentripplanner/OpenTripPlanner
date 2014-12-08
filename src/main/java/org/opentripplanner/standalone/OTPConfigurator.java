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
import java.util.List;
import java.util.prefs.Preferences;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.opentripplanner.graph_builder.GraphBuilderTask;
import org.opentripplanner.graph_builder.impl.EmbeddedConfigGraphBuilderImpl;
import org.opentripplanner.graph_builder.impl.GtfsGraphBuilderImpl;
import org.opentripplanner.graph_builder.impl.PruneFloatingIslands;
import org.opentripplanner.graph_builder.impl.StreetfulStopLinker;
import org.opentripplanner.graph_builder.impl.StreetlessStopLinker;
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
import org.opentripplanner.openstreetmap.impl.AnyFileBasedOpenStreetMapProviderImpl;
import org.opentripplanner.openstreetmap.services.OpenStreetMapProvider;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.impl.DefaultFareServiceFactory;
import org.opentripplanner.routing.impl.GraphScanner;
import org.opentripplanner.routing.impl.GraphServiceImpl;
import org.opentripplanner.routing.impl.InputStreamGraphSource;
import org.opentripplanner.routing.impl.MemoryGraphSource;
import org.opentripplanner.routing.services.GraphService;
import org.opentripplanner.updater.PropertiesPreferences;
import org.opentripplanner.visualizer.GraphVisualizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;

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
        GraphServiceImpl graphService = new GraphServiceImpl(params.autoReload);
        this.graphService = graphService;
        InputStreamGraphSource.FileFactory graphSourceFactory = new InputStreamGraphSource.FileFactory();
        graphService.graphSourceFactory = graphSourceFactory;
        if (params.graphDirectory != null) {
            graphSourceFactory.basePath = new File(params.graphDirectory);
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
        if (params.routerIds.size() > 0 || params.autoScan) {
            /* Auto-register pre-existing graph on disk, with optional auto-scan. */
            GraphScanner graphScanner = new GraphScanner(graphService, params.autoScan);
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
            // When using the long distance path service, or when there is no street data,
            // link stops to each other based on distance only, unless user has requested linking
            // based on transfers.txt or the street network (if available).
            if ((!hasOSM ) || params.longDistance) {
                if (!params.useTransfersTxt) {
                    if (!hasOSM || !params.useStreetsForLinking) {
                        graphBuilder.addGraphBuilder(new StreetlessStopLinker());
                    }
                }
            } 
            if ( hasOSM ) {
                graphBuilder.addGraphBuilder(new TransitToTaggedStopsGraphBuilderImpl());
                graphBuilder.addGraphBuilder(new TransitToStreetNetworkGraphBuilderImpl());
                // The stops can be linked to each other once they have links to the street network.
                if (params.longDistance && params.useStreetsForLinking && !params.useTransfersTxt) {
                    graphBuilder.addGraphBuilder(new StreetfulStopLinker());
                }
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
            GraphVisualizer visualizer = new GraphVisualizer(getGraphService().getGraph());
            return visualizer;
        } else return null;
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
