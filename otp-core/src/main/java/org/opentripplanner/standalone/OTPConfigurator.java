package org.opentripplanner.standalone;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.opentripplanner.analyst.core.GeometryIndex;
import org.opentripplanner.analyst.request.IsoChroneSPTRendererAccSampling;
import org.opentripplanner.analyst.request.IsoChroneSPTRendererRecursiveGrid;
import org.opentripplanner.analyst.request.Renderer;
import org.opentripplanner.analyst.request.SPTCache;
import org.opentripplanner.analyst.request.SampleFactory;
import org.opentripplanner.analyst.request.TileCache;
import org.opentripplanner.api.resource.PlanGenerator;
import org.opentripplanner.api.resource.services.MetadataService;
import org.opentripplanner.graph_builder.GraphBuilderTask;
import org.opentripplanner.graph_builder.impl.CongestionGraphBuilder;
import org.opentripplanner.graph_builder.impl.EmbeddedConfigGraphBuilderImpl;
import org.opentripplanner.graph_builder.impl.GtfsGraphBuilderImpl;
import org.opentripplanner.graph_builder.impl.PruneFloatingIslands;
import org.opentripplanner.graph_builder.impl.StreetlessStopLinker;
import org.opentripplanner.graph_builder.impl.TransitToStreetNetworkGraphBuilderImpl;
import org.opentripplanner.graph_builder.impl.ned.GeotiffGridCoverageFactoryImpl;
import org.opentripplanner.graph_builder.impl.ned.NEDGraphBuilderImpl;
import org.opentripplanner.graph_builder.impl.ned.NEDGridCoverageFactoryImpl;
import org.opentripplanner.graph_builder.impl.osm.DefaultWayPropertySetSource;
import org.opentripplanner.graph_builder.impl.osm.OpenStreetMapGraphBuilderImpl;
import org.opentripplanner.graph_builder.impl.transit_index.TransitIndexBuilder;
import org.opentripplanner.graph_builder.model.GtfsBundle;
import org.opentripplanner.graph_builder.services.GraphBuilder;
import org.opentripplanner.graph_builder.services.GraphBuilderWithGtfsDao;
import org.opentripplanner.graph_builder.services.ned.NEDGridCoverageFactory;
import org.opentripplanner.openstreetmap.impl.AnyFileBasedOpenStreetMapProviderImpl;
import org.opentripplanner.openstreetmap.services.OpenStreetMapProvider;
import org.opentripplanner.routing.algorithm.GenericAStar;
import org.opentripplanner.routing.core.RoutingRequest;
import org.opentripplanner.routing.impl.DefaultFareServiceFactory;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.impl.DefaultRemainingWeightHeuristicFactoryImpl;
import org.opentripplanner.routing.impl.GraphServiceBeanImpl;
import org.opentripplanner.routing.impl.GraphServiceImpl;
import org.opentripplanner.routing.impl.RetryingPathServiceImpl;
import org.opentripplanner.routing.impl.LongDistancePathService;
import org.opentripplanner.routing.services.GraphService;
import org.opentripplanner.routing.services.PathService;
import org.opentripplanner.routing.services.RemainingWeightHeuristicFactory;
import org.opentripplanner.routing.services.SPTService;
import org.opentripplanner.visualizer.GraphVisualizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;

public class OTPConfigurator {

    private static final Logger LOG = LoggerFactory.getLogger(OTPConfigurator.class);
    
    private final CommandLineParameters params;
    
    private GraphService graphService = null;
    
    private OTPComponentProviderFactory componentProviderFactory = null;
    
    public OTPConfigurator (CommandLineParameters params) {
        this.params = params;
    }

    /** 
     * Return an adapter that makes Jersey see OTP as a dependency injection framework. 
     * This will associate our specific OTP component instances with their interface classes.
     * 
     * We could even do this at Configurator construct time (rather than lazy initializing), using 
     * the inMemory param to create the right kind of graphservice ahead of time. However that 
     * would create indexes even when only a build was going to happen. 
     */
    public OTPComponentProviderFactory getComponentProviderFactory() {
        
        if (componentProviderFactory != null)
            return componentProviderFactory;
        
        LOG.info("Wiring up and configuring server task.");
        
        
        // Core OTP modules
        OTPComponentProviderFactory cpf = new OTPComponentProviderFactory(); 
        cpf.bind(GraphService.class, getGraphService());
        cpf.bind(RoutingRequest.class);
        cpf.bind(PlanGenerator.class);
        cpf.bind(MetadataService.class);
        cpf.bind(SPTService.class, new GenericAStar());
        
        // Choose a PathService to wrap the SPTService, depending on expected maximum path lengths
        if (params.longDistance) {
            LongDistancePathService pathService = new LongDistancePathService();
            pathService.setTimeout(10);
            cpf.bind(PathService.class, pathService);
        } else {
            RetryingPathServiceImpl pathService = new RetryingPathServiceImpl();
            pathService.setFirstPathTimeout(10.0);
            pathService.setMultiPathTimeout(1.0);
            cpf.bind(PathService.class, pathService);
            cpf.bind(RemainingWeightHeuristicFactory.class, 
                    new DefaultRemainingWeightHeuristicFactoryImpl()); 
        }
        
        // Optional Analyst Modules
        if (params.analyst) {
            cpf.bind(Renderer.class);
            cpf.bind(SPTCache.class);
            cpf.bind(TileCache.class);
            cpf.bind(GeometryIndex.class);
            cpf.bind(SampleFactory.class);
            cpf.bind(IsoChroneSPTRendererAccSampling.class);
            cpf.bind(IsoChroneSPTRendererRecursiveGrid.class);
        }
        
        // Perform field injection on bound instances and call post-construct methods
        cpf.doneBinding();   
        
        this.componentProviderFactory = cpf;
        return cpf;         
        
    }

    /** Create a cached GraphService that will be shared between all OTP components. */
    public void makeGraphService(Graph graph) {
        /* Hand off graph in memory to server in a single-graph in-memory GraphServiceImpl. */
        if (graph != null && params.inMemory) {
            this.graphService = new GraphServiceBeanImpl(graph);
        } else {
            /* Create a conventional GraphService that loads graphs from disk. */
            GraphServiceImpl graphService = new GraphServiceImpl();
            if (params.graphDirectory != null) {
                graphService.setPath(params.graphDirectory);
            }
            if (params.routerIds.size() > 0) {
                graphService.setDefaultRouterId(params.routerIds.get(0));
                graphService.setAutoRegister(params.routerIds);
            }
            this.graphService = graphService;
        }
    }

    /** Return the cached, shared GraphService, making one as needed. */
    public GraphService getGraphService () {
        if (graphService == null)
            makeGraphService(null);
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
            DefaultWayPropertySetSource defaultWayPropertySetSource = new DefaultWayPropertySetSource();
            osmBuilder.setDefaultWayPropertySetSource(defaultWayPropertySetSource);
            graphBuilder.addGraphBuilder(osmBuilder);
            graphBuilder.addGraphBuilder(new PruneFloatingIslands());
            if (params.congestionCsv != null) {
                graphBuilder.addGraphBuilder(new CongestionGraphBuilder(params.congestionCsv));
            }
        }
        if ( hasGTFS ) {
            List<GtfsBundle> gtfsBundles = Lists.newArrayList();
            for (File gtfsFile : gtfsFiles) {
                GtfsBundle gtfsBundle = new GtfsBundle(gtfsFile);
                gtfsBundle.setTransfersTxtDefinesStationPaths(params.useTransfersTxt);
                if (!params.noParentStopLinking) {
                    gtfsBundle.setLinkStopsToParentStations(true);
                }
                gtfsBundle.setParentStationTransfers(params.parentStationTransfers);
                gtfsBundles.add(gtfsBundle);
            }
            GtfsGraphBuilderImpl gtfsBuilder = new GtfsGraphBuilderImpl(gtfsBundles);
            graphBuilder.addGraphBuilder(gtfsBuilder);
            // When using the simplified path service, or when there is no street data,
            // link stops to each other based on distance only, unless user has requested linking
            // based on transfers.txt.
            if ( ( ! hasOSM ) || params.longDistance ) {
                if ( ! params.useTransfersTxt) {
                    graphBuilder.addGraphBuilder(new StreetlessStopLinker());
                }
            } 
            if ( hasOSM ) {
                graphBuilder.addGraphBuilder(new TransitToStreetNetworkGraphBuilderImpl());
            }
            List<GraphBuilderWithGtfsDao> gtfsBuilders = new ArrayList<GraphBuilderWithGtfsDao>();
            if (params.transitIndex) {
                gtfsBuilders.add(new TransitIndexBuilder());
            }
            gtfsBuilder.setFareServiceFactory(new DefaultFareServiceFactory());
            gtfsBuilder.setGtfsGraphBuilders(gtfsBuilders);
        }
        if (configFile != null) {
            EmbeddedConfigGraphBuilderImpl embeddedConfigBuilder = new EmbeddedConfigGraphBuilderImpl();
            embeddedConfigBuilder.setPropertiesFile(configFile);
            graphBuilder.addGraphBuilder(embeddedConfigBuilder);
        }
        if (params.elevation) {
            File cacheDirectory = new File(params.cacheDirectory, "ned");
            NEDGridCoverageFactory ngcf = new NEDGridCoverageFactoryImpl(cacheDirectory);
            GraphBuilder nedBuilder = new NEDGraphBuilderImpl(ngcf);
            graphBuilder.addGraphBuilder(nedBuilder);
        } else  if (demFile != null) {
            NEDGridCoverageFactory gcf = new GeotiffGridCoverageFactoryImpl(demFile);
            GraphBuilder nedBuilder = new NEDGraphBuilderImpl(gcf);
            graphBuilder.addGraphBuilder(nedBuilder);
        }
        graphBuilder.setSerializeGraph( ! params.inMemory);
        return graphBuilder;
    }

    public GrizzlyServer serverFromParameters() {
        if (params.server) {
            OTPComponentProviderFactory cpf = getComponentProviderFactory();
            GrizzlyServer server = new GrizzlyServer(cpf, params);
            return server;
        } else return null;
    }
    
    public GraphVisualizer visualizerFromParameters() {
        if (params.visualize) {
            @SuppressWarnings("unused") // get a component provider factory to force injection/post-construct
			OTPComponentProviderFactory cpf = getComponentProviderFactory();
            GraphVisualizer visualizer = new GraphVisualizer(getGraphService());
            return visualizer;
        } else return null;
    }

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
