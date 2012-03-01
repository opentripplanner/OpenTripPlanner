package org.opentripplanner.graph_builder.impl;

import java.util.HashMap;

import org.opentripplanner.common.StreetUtils;
import org.opentripplanner.graph_builder.services.GraphBuilder;
import org.opentripplanner.routing.graph.Graph;

public class PruneFloatingIslands implements GraphBuilder {

    @Override
    public void buildGraph(Graph graph, HashMap<Class<?>, Object> extra) {
        StreetUtils.pruneFloatingIslands(graph);
    }

}
