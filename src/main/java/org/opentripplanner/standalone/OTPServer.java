package org.opentripplanner.standalone;

import com.google.common.collect.Maps;
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
import org.opentripplanner.api.resource.services.MetadataService;
import org.opentripplanner.routing.algorithm.GenericAStar;
import org.opentripplanner.routing.core.RoutingRequest;
import org.opentripplanner.routing.impl.LongDistancePathService;
import org.opentripplanner.routing.impl.RetryingPathServiceImpl;
import org.opentripplanner.routing.services.GraphService;
import org.opentripplanner.routing.services.PathService;
import org.opentripplanner.routing.services.SPTService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
    public RoutingRequest routingRequest;
    public PlanGenerator planGenerator;
    public MetadataService metadataService;
    public SPTService sptService;

    // Optional Analyst Modules
    protected Renderer renderer;
    protected SPTCache sptCache;
    protected TileCache tileCache;
    protected GeometryIndex geometryIndex;
    protected SampleFactory sampleFactory;
    protected IsoChroneSPTRenderer isoChroneSPTRenderer;
    protected SampleGridRenderer sampleGridRenderer;

    public Router getRouter(String routerId) {
        return routers.get(routerId);
    }

    public OTPServer (CommandLineParameters params, GraphService gs) {
        LOG.info("Wiring up and configuring server.");

        // Core OTP modules
        graphService = gs;
        routingRequest = new RoutingRequest();
        sptService = new GenericAStar();
        metadataService = new MetadataService(graphService);

        // Choose a PathService to wrap the SPTService, depending on expected maximum path lengths
        if (params.longDistance) {
            LongDistancePathService pathService = new LongDistancePathService();
            pathService.setTimeout(10);
            this.pathService = pathService;
        } else {
            RetryingPathServiceImpl pathService = new RetryingPathServiceImpl();
            pathService.setFirstPathTimeout(10.0);
            pathService.setMultiPathTimeout(1.0);
            this.pathService = pathService;
            // cpf.bind(RemainingWeightHeuristicFactory.class,
            //        new DefaultRemainingWeightHeuristicFactoryImpl());
        }

        planGenerator = new PlanGenerator(graphService, pathService);

        // Optional Analyst Modules
        if (params.analyst) {
            sampleFactory = new SampleFactory();
            geometryIndex = new GeometryIndex(graphService);
            tileCache = new TileCache(sampleFactory);
            sptCache = new SPTCache(sptService, graphService);
            renderer = new Renderer(tileCache, sptCache);
            sampleGridRenderer = new SampleGridRenderer(graphService, sptService);
            isoChroneSPTRenderer = new IsoChroneSPTRendererAccSampling(graphService, sptService, sampleGridRenderer);
        }

    }

}
