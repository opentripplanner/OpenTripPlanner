package org.opentripplanner.standalone;

import java.io.File;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

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
import org.opentripplanner.graph_builder.impl.osm.OpenStreetMapGraphBuilderImpl;
import org.opentripplanner.graph_builder.model.GtfsBundle;
import org.opentripplanner.graph_builder.services.GraphBuilder;
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

/*
 * TODO Make the methods non-static, construct this class with a ComandLineParameters, 
 * and add an "infer" step that sets some extra boolean variables based on the command line parameters.
 */
public class OTPConfigurator {

    private static final Logger LOG = LoggerFactory.getLogger(OTPConfigurator.class);

    private static final String DEFAULT_GRAPH_LOCATION = "/var/otp/graphs";
    
    /**
     * @param graph if non-null, the in-memry graph to use (rather than loading from disk)
     */
    public static OTPComponentProviderFactory providerFromParameters(CommandLineParameters params, Graph graph) {
        
        LOG.info("Wiring up and configuring server task.");
        
        // The PathService which wraps the SPTService
        RetryingPathServiceImpl pathService = new RetryingPathServiceImpl();
        pathService.setFirstPathTimeout(10.0);
        pathService.setMultiPathTimeout(1.0);
        
        // An adapter to make Jersey see OTP as a dependency injection framework.
        // Associate our specific instances with their interface classes.
        OTPComponentProviderFactory cpf = new OTPComponentProviderFactory(); 
        cpf.bind(GraphService.class, makeGraphService(params, graph));
        cpf.bind(RoutingRequest.class);
        cpf.bind(PlanGenerator.class);
        cpf.bind(MetadataService.class);
        cpf.bind(SPTService.class, new GenericAStar());
        cpf.bind(PathService.class, pathService);
        cpf.bind(RemainingWeightHeuristicFactory.class, 
                new DefaultRemainingWeightHeuristicFactoryImpl()); 

        // Optional Analyst Modules
        cpf.bind(Renderer.class);
        cpf.bind(SPTCache.class);
        cpf.bind(TileCache.class);
        cpf.bind(GeometryIndex.class);
        cpf.bind(SampleFactory.class);
        
        // Perform field injection on bound instances and call post-construct methods
        cpf.doneBinding();        
        return cpf;         
        
    }

    private static GraphService makeGraphService(CommandLineParameters params, Graph graph) {
        if (graph != null) {
            return new GraphServiceBeanImpl(graph);
        }
        GraphServiceImpl graphService = new GraphServiceImpl();
        if (params.graphDirectory != null) {
            graphService.setPath(params.graphDirectory);
        } else {
            graphService.setPath(DEFAULT_GRAPH_LOCATION);
        }
        if (params.defaultRouterId != null) {
            graphService.setDefaultRouterId(params.defaultRouterId);
        }
        return graphService;
    }

    public static GraphBuilderTask builderFromParameters(CommandLineParameters params) {
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
        boolean hasOSM  = ! osmFiles.isEmpty();
        boolean hasGTFS = ! gtfsFiles.isEmpty();
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
        graphBuilder.setSerializeGraph( ! params.inMemory);
        return graphBuilder;
    }

    public static GrizzlyServer serverFromParameters(CommandLineParameters params, Graph graph) {
        OTPComponentProviderFactory cpf = providerFromParameters(params, graph);
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
