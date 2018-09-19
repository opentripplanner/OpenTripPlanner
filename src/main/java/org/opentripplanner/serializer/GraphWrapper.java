package org.opentripplanner.serializer;

import org.opentripplanner.graph_builder.annotation.GraphBuilderAnnotation;
import org.opentripplanner.routing.graph.Edge;
import org.opentripplanner.routing.graph.Graph;

import java.util.Collection;
import java.util.List;

/**
 * Implemented because of edges transient in graph object and is serialized/deserialized separately.
 */
public class GraphWrapper {
    public List<Edge> edges;
    public Graph graph;
}
