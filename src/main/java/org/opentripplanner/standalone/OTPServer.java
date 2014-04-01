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
import org.opentripplanner.routing.core.RoutingRequest;
import org.opentripplanner.routing.services.GraphService;
import org.opentripplanner.routing.services.SPTService;

import java.util.Map;

/**
 *
 */
public class OTPServer {

    // will replace graphService
    private final Map<String, Router> routers = Maps.newHashMap();

    // Core OTP modules
    public GraphService graphService;
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

    public OTPServer () {
        // wire the thing up
    }

}
