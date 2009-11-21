package org.opentripplanner.graph_builder;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.opentripplanner.graph_builder.services.GraphBuilder;
import org.opentripplanner.model.GraphBundle;
import org.opentripplanner.routing.core.Graph;
import org.opentripplanner.routing.impl.GraphSerializationLibrary;
import org.springframework.beans.factory.annotation.Autowired;

public class GraphBuilderTask {

    private Graph _graph;

    private List<GraphBuilder> _graphBuilders = new ArrayList<GraphBuilder>();

    private GraphBundle _graphBundle;

    @Autowired
    public void setGraph(Graph graph) {
        _graph = graph;
    }

    public void addGraphBuilder(GraphBuilder loader) {
        _graphBuilders.add(loader);
    }

    public void setGraphBuilders(List<GraphBuilder> graphLoaders) {
        _graphBuilders = graphLoaders;
    }

    public void setGraphBundle(GraphBundle graphBundle) {
        _graphBundle = graphBundle;
    }

    public void run() throws IOException {
        for (GraphBuilder load : _graphBuilders)
            load.buildGraph(_graph);
        GraphSerializationLibrary.writeGraph(_graph, _graphBundle.getGraphPath());
    }
}
