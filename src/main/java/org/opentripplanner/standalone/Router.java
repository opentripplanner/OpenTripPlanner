package org.opentripplanner.standalone;

import org.opentripplanner.analyst.request.IsoChroneSPTRenderer;
import org.opentripplanner.analyst.request.Renderer;
import org.opentripplanner.analyst.request.SPTCache;
import org.opentripplanner.analyst.request.SampleGridRenderer;
import org.opentripplanner.analyst.request.TileCache;
import org.opentripplanner.api.resource.PlanGenerator;
import org.opentripplanner.inspector.TileRendererManager;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.impl.SPTServiceFactory;
import org.opentripplanner.routing.services.PathService;

/**
 * Represents the configuration of a single router (a single graph for a specific geographic area)
 * in an OTP server.
 */
public class Router {

    public String id;
    public Graph graph;

    // Core services
    public PlanGenerator planGenerator;
    public PathService pathService;
    public SPTServiceFactory sptServiceFactory;

    // Inspector/debug services
    public TileRendererManager tileRendererManager;

    // Analyst services
    public SPTCache sptCache;
    public TileCache tileCache;
    public Renderer renderer;
    public IsoChroneSPTRenderer isoChroneSPTRenderer;
    public SampleGridRenderer sampleGridRenderer;

    public Router(String id, Graph graph) {
        this.id = id;
        this.graph = graph;
    }

}
