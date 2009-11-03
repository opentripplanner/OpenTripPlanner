package org.opentripplanner.jags.spt;

import java.util.Vector;

public class GraphPath {
    public Vector<SPTVertex> vertices;

    public Vector<SPTEdge> edges;

    public GraphPath() {
        this.vertices = new Vector<SPTVertex>();
        this.edges = new Vector<SPTEdge>();
    }

    public String toString() {
        return vertices.toString();
    }
}