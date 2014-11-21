package org.opentripplanner.standalone;

import org.opentripplanner.api.resource.PlanGenerator;
import org.opentripplanner.inspector.TileRendererManager;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.services.PathService;

/**
 * Represents the configuration of a single router (a single graph for a specific geographic area)
 * in an OTP server.
 */
public class Router {

    public String id;
    public Graph graph;

    public PlanGenerator planGenerator;
    public PathService pathService;

    public TileRendererManager tileRendererManager;

    public Router(String id, Graph graph) {
        this.id = id;
        this.graph = graph;
    }

}
