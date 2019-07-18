package org.opentripplanner.routing.graph;

import java.io.Serializable;
import java.util.Collection;

/**
 * The Graph object does not contain a collection of edges. The set of edges is generated on demand from the vertices.
 * However, when serializing, we intentionally do not serialize the vertices' edge lists to prevent excessive recursion.
 * So we need to save the edges along with the graph. We used to make two serialization calls, one for the graph and
 * one for the edges. But we need the serializer to know that vertices referenced by the edges are the same vertices
 * stored in the graph itself. The easiest way to do this is to make only one serialization call, serializing a single
 * object that contains both the graph and the edge collection.
 */
public class GraphAndEdges implements Serializable {

    public final Graph graph;

    private final Collection<Edge> edges;

    public GraphAndEdges(Graph graph) {
        this.graph = graph;
        this.edges = graph.getEdges();
    }

    /**
     * After deserialization, the vertices will all have null outgoing and incoming edge lists because those edge lists
     * are marked transient, to prevent excessive recursion depth while serializing. This method will reconstruct all
     * those edge lists after deserialization.
     */
    public void reconstructEdgeLists() {
        for (Vertex v : graph.getVertices()) {
            v.initEdgeLists();
        }
        for (Edge e : edges) {
            Vertex fromVertex = e.getFromVertex();
            Vertex toVertex = e.getToVertex();
            fromVertex.addOutgoing(e);
            toVertex.addIncoming(e);
        }
    }

}
