package org.opentripplanner.api.model.analysis;

import java.util.List;

import org.codehaus.jackson.map.annotate.JsonSerialize;
import org.opentripplanner.api.model.json_serializers.VertexSetJSONSerializer;
import org.opentripplanner.api.model.json_serializers.WithGraph;
import org.opentripplanner.routing.core.Graph;
import org.opentripplanner.routing.core.Vertex;


public class VertexSet {
    public List<Vertex> vertices;

    @JsonSerialize(using=VertexSetJSONSerializer.class)
    class VertexSetWithGraph extends WithGraph {
        VertexSetWithGraph(Graph graph, VertexSet vertexSet) {
            super(graph, vertexSet);
        }
    }

    public VertexSetWithGraph withGraph(Graph graph) {
        return new VertexSetWithGraph(graph, this);
    }
}
