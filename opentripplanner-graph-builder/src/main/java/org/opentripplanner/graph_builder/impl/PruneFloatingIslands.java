package org.opentripplanner.graph_builder.impl;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import org.opentripplanner.common.StreetUtils;
import org.opentripplanner.graph_builder.services.GraphBuilder;
import org.opentripplanner.routing.graph.Graph;

public class PruneFloatingIslands implements GraphBuilder {

    public List<String> provides() {
        return Collections.emptyList();
    }

    public List<String> getPrerequisites() {
        return Arrays.asList("streets");
    }
    
    @Override
    public void buildGraph(Graph graph, HashMap<Class<?>, Object> extra) {
        StreetUtils.pruneFloatingIslands(graph);
    }

}
