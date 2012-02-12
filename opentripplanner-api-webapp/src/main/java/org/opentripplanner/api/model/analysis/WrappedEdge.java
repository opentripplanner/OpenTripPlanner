package org.opentripplanner.api.model.analysis;

import org.opentripplanner.routing.graph.Edge;

public class WrappedEdge {

    public Edge edge;

    public int id;

    public WrappedEdge() {

    }

    public WrappedEdge(Edge edge, int id) {
        this.edge = edge;
        this.id = id;
    }
}
