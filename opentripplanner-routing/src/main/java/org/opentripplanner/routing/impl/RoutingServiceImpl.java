package org.opentripplanner.routing.impl;

import java.util.HashMap;

import org.opentripplanner.routing.algorithm.AStar;
import org.opentripplanner.routing.core.Edge;
import org.opentripplanner.routing.core.Graph;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.core.TraverseOptions;
import org.opentripplanner.routing.core.Vertex;
import org.opentripplanner.routing.services.RoutingService;
import org.opentripplanner.routing.spt.GraphPath;
import org.opentripplanner.routing.spt.ShortestPathTree;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class RoutingServiceImpl implements RoutingService {

    private Graph _graph;

    @Autowired
    public void setGraph(Graph graph) {
        _graph = graph;
    }

    @Override
    public GraphPath route(Vertex fromVertex, Vertex toVertex, State state, TraverseOptions options) {

        HashMap<Vertex, Edge> extraEdges = new HashMap<Vertex, Edge>();

        ShortestPathTree spt = AStar.getShortestPathTree(_graph, fromVertex, toVertex, state,
                options, extraEdges);
        return spt.getPath(toVertex);
    }

}
