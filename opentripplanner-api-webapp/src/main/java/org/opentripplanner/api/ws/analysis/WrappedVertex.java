package org.opentripplanner.api.ws.analysis;

import org.codehaus.jackson.map.annotate.JsonSerialize;
import org.opentripplanner.api.model.json_serializers.VertexSetJSONSerializer;
import org.opentripplanner.api.model.json_serializers.WithGraph;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.graph.Vertex;

public class WrappedVertex {

    public Vertex vertex;

    public WrappedVertex() {
    }

    public WrappedVertex(Vertex vertex) {
        this.vertex = vertex;
    }
    
    public WithGraph withGraph(Graph graph) {
        return new WrappedVertexWithGraph(graph, this);
    }

    @JsonSerialize(using=VertexSetJSONSerializer.class)
    class WrappedVertexWithGraph extends WithGraph {
        WrappedVertexWithGraph(Graph graph, WrappedVertex vertex) {
            super(graph, vertex);
        }
    }
}
