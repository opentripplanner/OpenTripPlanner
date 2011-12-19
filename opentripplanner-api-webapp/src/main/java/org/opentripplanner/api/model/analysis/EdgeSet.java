package org.opentripplanner.api.model.analysis;

import java.util.List;

import org.codehaus.jackson.map.annotate.JsonSerialize;
import org.opentripplanner.api.model.json_serializers.EdgeSetJSONSerializer;
import org.opentripplanner.api.model.json_serializers.WithGraph;
import org.opentripplanner.routing.core.Graph;

public class EdgeSet {
    public List<WrappedEdge> edges;

    @JsonSerialize(using = EdgeSetJSONSerializer.class)
    class EdgeSetWithGraph extends WithGraph {
        EdgeSetWithGraph(Graph graph, EdgeSet edgeSet) {
            super(graph, edgeSet);
        }
    }

    public EdgeSetWithGraph withGraph(Graph graph) {
        return new EdgeSetWithGraph(graph, this);
    }
}
