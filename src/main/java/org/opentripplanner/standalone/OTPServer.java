package org.opentripplanner.standalone;

import java.io.File;
import java.util.Collection;

import org.glassfish.hk2.utilities.binding.AbstractBinder;
import org.opentripplanner.analyst.DiskBackedPointSetCache;
import org.opentripplanner.analyst.PointSetCache;
import org.opentripplanner.analyst.SurfaceCache;
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
import org.opentripplanner.routing.services.GraphService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This is replacing a Spring application context.
 */
public class OTPServer {

    private static final Logger LOG = LoggerFactory.getLogger(OTPServer.class);

    // Core OTP modules
    private GraphService graphService;

    /*
     * The prototype routing request which establishes default parameter values. Note: this need to
     * be server-wide as we build the request before knowing which router it will be resolved to.
     * This prevent from having default request values per router instance. Fix this if this is
     * needed.
     */
    public RoutingRequest routingRequest;

    // Optional Analyst global modules (caches)
    public SurfaceCache surfaceCache;
    public PointSetCache pointSetCache;

    public CommandLineParameters params;

    public OTPServer (CommandLineParameters params, GraphService gs) {
        LOG.info("Wiring up and configuring server.");

        this.params = params;

        // Core OTP modules
        graphService = gs;
        routingRequest = new RoutingRequest();

        // Optional Analyst Modules.
        if (params.analyst) {
            surfaceCache = new SurfaceCache(30);
            pointSetCache = new DiskBackedPointSetCache(100, new File(params.pointSetDirectory));
        }
    }

    /**
     * @return The GraphService. Please use it only when the GraphService itself is necessary. To
     *         get Graph instances, use getRouter().
     */
    public GraphService getGraphService() {
        return graphService;
    }

    /**
     * @return A list of all router IDs currently available.
     */
    public Collection<String> getRouterIds() {
        return graphService.getRouterIds();
    }

    public Router getRouter(String routerId) {
        /*
         * Note: We store the router "owning" the Graph in the graph itself, as a service. This
         * seems to be a bit hackish, but it seems to be the simplest solution to solve graph
         * ownership / management conflicts between OTPServer and GraphService. Anyway, we still
         * have to decide if a Router is appropriate for storing graph-related services (we store
         * some services in Graph, some other in Router). Is the distinction based on wether a
         * service is serialized?
         */

        Graph graph = graphService.getGraph(routerId);
        /* Note: if graph does not exists, a GraphNotFoundException will be thrown. */

        /* Performance impact of the synchronized below should be OK. */
        synchronized (graph) {
            Router router = graph.getService(Router.class);
            if (router == null) {
                router = createRouter(routerId, graph);
                graph.putService(Router.class, router);
            }
            return router;
        }
    }

    /**
     * Create a new Router, owning a Graph and all it's associated services.
     */
    private Router createRouter(String routerId, Graph graph) {
        Router router = new Router(routerId, graph);

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
        router.planGenerator = new PlanGenerator(graph, router.pathService);
        router.tileRendererManager = new TileRendererManager(graph);

        // Optional Analyst Modules.
        if (params.analyst) {
            router.tileCache = new TileCache(router.graph);
            router.sptCache = new SPTCache(router.sptServiceFactory, graph);
            router.renderer = new Renderer(router.tileCache, router.sptCache);
            router.sampleGridRenderer = new SampleGridRenderer(router.graph,
                    router.sptServiceFactory);
            router.isoChroneSPTRenderer = new IsoChroneSPTRendererAccSampling(
                    router.sampleGridRenderer);
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
