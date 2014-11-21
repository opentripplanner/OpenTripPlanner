package org.opentripplanner.standalone;

import java.io.File;
import java.util.Map;

import org.glassfish.hk2.utilities.binding.AbstractBinder;
import org.opentripplanner.analyst.DiskBackedPointSetCache;
import org.opentripplanner.analyst.PointSetCache;
import org.opentripplanner.analyst.SurfaceCache;
import org.opentripplanner.analyst.request.IsoChroneSPTRenderer;
import org.opentripplanner.analyst.request.IsoChroneSPTRendererAccSampling;
import org.opentripplanner.analyst.request.Renderer;
import org.opentripplanner.analyst.request.SPTCache;
import org.opentripplanner.analyst.request.SampleGridRenderer;
import org.opentripplanner.analyst.request.TileCache;
import org.opentripplanner.api.resource.PlanGenerator;
import org.opentripplanner.inspector.TileRendererManager;
import org.opentripplanner.routing.core.RoutingRequest;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.impl.GenericAStarFactory;
import org.opentripplanner.routing.impl.LongDistancePathService;
import org.opentripplanner.routing.impl.RetryingPathServiceImpl;
import org.opentripplanner.routing.impl.SPTServiceFactory;
import org.opentripplanner.routing.services.GraphService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Maps;

/**
 * This is replacing a Spring application context.
 */
public class OTPServer {

    private static final Logger LOG = LoggerFactory.getLogger(OTPServer.class);

    // Map router ID -> active Routers
    private final Map<String, Router> routers = Maps.newHashMap();

    // Core OTP modules
    public GraphService graphService;

    /*
     * The prototype routing request which establishes default parameter values. Note: this need to
     * be server-wide as we build the request before knowing which router it will be resolved to.
     * This prevent from having default request values per router instance. TODO Fix this if this is
     * needed.
     */
    public RoutingRequest routingRequest;
    public SPTServiceFactory sptServiceFactory;

    // Optional Analyst Modules
    public IsoChroneSPTRenderer isoChroneSPTRenderer;
    public SampleGridRenderer sampleGridRenderer;
    public SurfaceCache surfaceCache;
    public PointSetCache pointSetCache;

    public CommandLineParameters params;

    public Router getRouter(String routerId) {
        /*
         * TODO Maybe Router should be owned by GraphService? Here we have two routerId mapping: one
         * for graph, one for router.
         */
        Graph graph = graphService.getGraph(routerId);
        /* Performance impact of the synchronized below should be OK. */
        synchronized(routers) {
            if (graph == null) {
                routers.remove(routerId);
                return null;
            }
            Router router = routers.get(routerId);
            /*
             * Note: We re-create a new Router if none exists or if graph instance changed
             * since we created the first router. Note that we use pointer compare, not equals.
             */
            if (router == null || router.graph != graph) {
                router = createRouter(routerId, graph);
                routers.put(routerId, router);
            }
            return router;
        }
    }

    public OTPServer (CommandLineParameters params, GraphService gs) {
        LOG.info("Wiring up and configuring server.");

        this.params = params; 

        // Core OTP modules
        graphService = gs;
        routingRequest = new RoutingRequest();
        sptServiceFactory = new GenericAStarFactory();

        // Optional Analyst Modules.
        if (params.analyst) {
            sampleGridRenderer = new SampleGridRenderer(graphService, sptServiceFactory);
            isoChroneSPTRenderer = new IsoChroneSPTRendererAccSampling(graphService, sptServiceFactory, sampleGridRenderer);
            surfaceCache = new SurfaceCache(30);
            pointSetCache = new DiskBackedPointSetCache(100, new File(params.pointSetDirectory));
        }
    }

    /**
     * Create a new Router, owning a Graph and all it's associated services.
     */
    private Router createRouter(String routerId, Graph graph) {
        Router router = new Router(routerId, graph);

        // Choose a PathService to wrap the SPTService, depending on expected maximum path lengths
        if (params.longDistance) {
            LongDistancePathService pathService = new LongDistancePathService(graphService, sptServiceFactory);
            pathService.timeout = 10;
            router.pathService = pathService;
        } else {
            RetryingPathServiceImpl pathService = new RetryingPathServiceImpl(graphService, sptServiceFactory);
            pathService.setFirstPathTimeout(10.0);
            pathService.setMultiPathTimeout(1.0);
            router.pathService = pathService;
            // cpf.bind(RemainingWeightHeuristicFactory.class,
            //        new DefaultRemainingWeightHeuristicFactoryImpl());
        }
        router.planGenerator = new PlanGenerator(graph, router.pathService);
        router.tileRendererManager = new TileRendererManager(graph);

        // Optional Analyst Modules.
        if (params.analyst) {
            router.tileCache = new TileCache(router.graph);
            router.sptCache = new SPTCache(sptServiceFactory, graph);
            router.renderer = new Renderer(router.tileCache, router.sptCache);
        }

        return router;
    }

    /**
     * Return an HK2 Binder that injects this specific OTPServer instance into Jersey web resources.
     * This should be registered in the ResourceConfig (Jersey) or Application (JAX-RS) as a singleton.
     * More on custom injection in Jersey 2:
     * http://jersey.576304.n2.nabble.com/Custom-providers-in-Jersey-2-tp7580699p7580715.html
     */
     public AbstractBinder makeBinder() {
        return new AbstractBinder() {
            @Override
            protected void configure() {
                bind(OTPServer.this).to(OTPServer.class);
            }
        };
    }

}
