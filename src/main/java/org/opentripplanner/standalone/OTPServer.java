package org.opentripplanner.standalone;

import com.google.common.collect.Maps;
import org.glassfish.hk2.utilities.binding.AbstractBinder;
import org.opentripplanner.analyst.PointSetCache;
import org.opentripplanner.analyst.SurfaceCache;
import org.opentripplanner.analyst.core.GeometryIndex;
import org.opentripplanner.analyst.request.IsoChroneSPTRenderer;
import org.opentripplanner.analyst.request.IsoChroneSPTRendererAccSampling;
import org.opentripplanner.analyst.request.IsoChroneSPTRendererRecursiveGrid;
import org.opentripplanner.analyst.request.Renderer;
import org.opentripplanner.analyst.request.SPTCache;
import org.opentripplanner.analyst.request.SampleFactory;
import org.opentripplanner.analyst.request.SampleGridRenderer;
import org.opentripplanner.analyst.request.TileCache;
import org.opentripplanner.api.resource.PlanGenerator;
import org.opentripplanner.routing.algorithm.GenericAStar;
import org.opentripplanner.routing.core.RoutingRequest;
import org.opentripplanner.routing.impl.LongDistancePathService;
import org.opentripplanner.routing.impl.RetryingPathServiceImpl;
import org.opentripplanner.routing.services.GraphService;
import org.opentripplanner.routing.services.PathService;
import org.opentripplanner.routing.services.SPTService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.ext.Provider;
import java.util.Map;

/**
 * This is replacing a Spring application context.
 */
public class OTPServer {

    private static final Logger LOG = LoggerFactory.getLogger(OTPServer.class);

    // will replace graphService
    private final Map<String, Router> routers = Maps.newHashMap();

    // Core OTP modules
    public GraphService graphService;
    public PathService pathService;
    public RoutingRequest routingRequest; // the prototype routing request which establishes default parameter values
    public PlanGenerator planGenerator;
    public SPTService sptService;

    // Optional Analyst Modules
    public Renderer renderer;
    public SPTCache sptCache;
    public TileCache tileCache;
    public GeometryIndex geometryIndex;
    public SampleFactory sampleFactory;
    public IsoChroneSPTRenderer isoChroneSPTRenderer;
    public SampleGridRenderer sampleGridRenderer;
    public SurfaceCache surfaceCache;
    public PointSetCache pointSetCache;

    public Router getRouter(String routerId) {
        return routers.get(routerId);
    }

    public OTPServer (CommandLineParameters params, GraphService gs) {
        LOG.info("Wiring up and configuring server.");

        // Core OTP modules
        graphService = gs;
        routingRequest = new RoutingRequest();
        sptService = new GenericAStar();

        // Choose a PathService to wrap the SPTService, depending on expected maximum path lengths
        if (params.longDistance) {
            LongDistancePathService pathService = new LongDistancePathService(graphService, sptService);
            pathService.setTimeout(10);
            this.pathService = pathService;
        } else {
            RetryingPathServiceImpl pathService = new RetryingPathServiceImpl(graphService, sptService);
            pathService.setFirstPathTimeout(10.0);
            pathService.setMultiPathTimeout(1.0);
            this.pathService = pathService;
            // cpf.bind(RemainingWeightHeuristicFactory.class,
            //        new DefaultRemainingWeightHeuristicFactoryImpl());
        }

        planGenerator = new PlanGenerator(graphService, pathService);

        // Optional Analyst Modules. They only work with default graph for now.
        if (params.analyst) {
            geometryIndex = new GeometryIndex(graphService);
            sampleFactory = new SampleFactory(geometryIndex);
            tileCache = new TileCache(sampleFactory);
            sptCache = new SPTCache(sptService, graphService);
            renderer = new Renderer(tileCache, sptCache);
            sampleGridRenderer = new SampleGridRenderer(graphService, sptService);
            isoChroneSPTRenderer = new IsoChroneSPTRendererAccSampling(graphService, sptService, sampleGridRenderer);
            surfaceCache = new SurfaceCache(20);
            pointSetCache = new PointSetCache(sampleFactory);
        }

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
