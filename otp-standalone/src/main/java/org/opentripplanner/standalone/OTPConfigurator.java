package org.opentripplanner.standalone;

import java.io.File;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import lombok.Setter;

import org.opentripplanner.analyst.core.GeometryIndex;
import org.opentripplanner.analyst.request.Renderer;
import org.opentripplanner.analyst.request.SPTCache;
import org.opentripplanner.analyst.request.SampleFactory;
import org.opentripplanner.analyst.request.TileCache;
import org.opentripplanner.api.ws.PlanGenerator;
import org.opentripplanner.api.ws.services.MetadataService;
import org.opentripplanner.graph_builder.GraphBuilderTask;
import org.opentripplanner.graph_builder.impl.GtfsGraphBuilderImpl;
import org.opentripplanner.graph_builder.impl.StreetlessStopLinker;
import org.opentripplanner.graph_builder.impl.TransitToStreetNetworkGraphBuilderImpl;
import org.opentripplanner.graph_builder.impl.ned.NEDGraphBuilderImpl;
import org.opentripplanner.graph_builder.impl.ned.NEDGridCoverageFactoryImpl;
import org.opentripplanner.graph_builder.impl.osm.OpenStreetMapGraphBuilderImpl;
import org.opentripplanner.graph_builder.model.GtfsBundle;
import org.opentripplanner.graph_builder.services.GraphBuilder;
import org.opentripplanner.graph_builder.services.ned.NEDGridCoverageFactory;
import org.opentripplanner.openstreetmap.impl.AnyFileBasedOpenStreetMapProviderImpl;
import org.opentripplanner.openstreetmap.services.OpenStreetMapProvider;
import org.opentripplanner.routing.algorithm.GenericAStar;
import org.opentripplanner.routing.core.RoutingRequest;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.impl.DefaultRemainingWeightHeuristicFactoryImpl;
import org.opentripplanner.routing.impl.GraphServiceBeanImpl;
import org.opentripplanner.routing.impl.GraphServiceImpl;
import org.opentripplanner.routing.impl.RetryingPathServiceImpl;
import org.opentripplanner.routing.services.GraphService;
import org.opentripplanner.routing.services.PathService;
import org.opentripplanner.routing.services.RemainingWeightHeuristicFactory;
import org.opentripplanner.routing.services.SPTService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;

public class OTPConfigurator {

    private static final Logger LOG = LoggerFactory.getLogger(OTPConfigurator.class);
    
    private final CommandLineParameters params;
    
    /* If non-null, the in-memory graph to use (rather than loading from disk) */
    @Setter private Graph graph;

    public OTPConfigurator (CommandLineParameters params) {
        this.params = params;
    }

    /** 
     * Create an adapter to make Jersey see OTP as a dependency injection framework. 
     * This will associate our specific OTP component instances with their interface classes.
     */
    public OTPComponentProviderFactory providerFromParameters() {
        
        LOG.info("Wiring up and configuring server task.");
        
        // The PathService which wraps the SPTService
        RetryingPathServiceImpl pathService = new RetryingPathServiceImpl();
        pathService.setFirstPathTimeout(10.0);
        pathService.setMultiPathTimeout(1.0);
        
        // Core OTP modules
        OTPComponentProviderFactory cpf = new OTPComponentProviderFactory(); 
        cpf.bind(GraphService.class, makeGraphService());
        cpf.bind(RoutingRequest.class);
        cpf.bind(PlanGenerator.class);
        cpf.bind(MetadataService.class);
        cpf.bind(SPTService.class, new GenericAStar());
        cpf.bind(PathService.class, pathService);
        cpf.bind(RemainingWeightHeuristicFactory.class, 
                new DefaultRemainingWeightHeuristicFactoryImpl()); 

        // Optional Analyst Modules
        if (params.analyst) {
            cpf.bind(Renderer.class);
            cpf.bind(SPTCache.class);
            cpf.bind(TileCache.class);
            cpf.bind(GeometryIndex.class);
            cpf.bind(SampleFactory.class);
        }
        
        // Perform field injection on bound instances and call post-construct methods
        cpf.doneBinding();        
        return cpf;         
        
    }

    private GraphService makeGraphService() {
        /* Hand off graph in memory to server in a single-graph in-memory GraphServiceImpl. */
        if (graph != null && params.inMemory) {
            return new GraphServiceBeanImpl(graph);
        }
        /* Create a conventional GraphService that loads graphs from disk. */
        GraphServiceImpl graphService = new GraphServiceImpl();
        if (params.graphDirectory != null) {
            graphService.setPath(params.graphDirectory);
        }
        if (params.defaultRouterId != null) {
            graphService.setDefaultRouterId(params.defaultRouterId);
        }
        return graphService;
    }

    public GraphBuilderTask builderFromParameters() {
        LOG.info("Wiring up and configuring graph builder task.");
        if (params.build == null || params.build.isEmpty()) {
            return null;
        }
        GraphBuilderTask graphBuilder = new GraphBuilderTask();
        List<File> gtfsFiles = Lists.newArrayList();
        List<File> osmFiles =  Lists.newArrayList();
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
            GraphBuilder osmBuilder = new OpenStreetMapGraphBuilderImpl(osmProviders); 
            graphBuilder.addGraphBuilder(osmBuilder);
        }
        if ( hasGTFS ) {
            List<GtfsBundle> gtfsBundles = Lists.newArrayList();
            for (File gtfsFile : gtfsFiles) {
                GtfsBundle gtfsBundle = new GtfsBundle(gtfsFile);
                gtfsBundles.add(gtfsBundle);
            }
            GraphBuilder gtfsBuilder = new GtfsGraphBuilderImpl(gtfsBundles);
            graphBuilder.addGraphBuilder(gtfsBuilder);
            if ( hasOSM ) {
                graphBuilder.addGraphBuilder(new TransitToStreetNetworkGraphBuilderImpl());
            } else {
                graphBuilder.addGraphBuilder(new StreetlessStopLinker());
            }
        }
        if (params.elevation) {
            File cacheDirectory = new File(params.cacheDirectory, "ned");
            NEDGridCoverageFactory ngcf = new NEDGridCoverageFactoryImpl(cacheDirectory);
            GraphBuilder nedBuilder = new NEDGraphBuilderImpl(ngcf);
            graphBuilder.addGraphBuilder(nedBuilder);
        }
        graphBuilder.setSerializeGraph( ! params.inMemory);
        return graphBuilder;
    }

    public GrizzlyServer serverFromParameters() {
        OTPComponentProviderFactory cpf = providerFromParameters();
        GrizzlyServer server = new GrizzlyServer(cpf, params);
        return server;
    }
    
    private static enum InputFileType {
        GTFS, OSM, OTHER;
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
            return OTHER;
        }
    }

}
