package org.opentripplanner.api.model.json_serializers;

import org.opentripplanner.routing.graph.Graph;

public class WithGraph {
    private Graph graph;
    private Object object;
    public WithGraph(Graph graph, Object object) {
        this.graph = graph;
        this.object = object;
    }
    public Graph getGraph() {
        return graph;
    }
    
    public Object getObject() {
        return object;
    }
    
}
