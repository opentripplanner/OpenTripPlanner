package org.opentripplanner.routing.services;

import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.core.TraverseOptions;
import org.opentripplanner.routing.core.Vertex;
import org.opentripplanner.routing.spt.GraphPath;

public interface RoutingService {

    public GraphPath route(Vertex fromVertex, Vertex toVertex, State state, TraverseOptions options);
}
