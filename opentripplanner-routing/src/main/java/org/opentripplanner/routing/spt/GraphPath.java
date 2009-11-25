package org.opentripplanner.routing.spt;

import java.util.Collections;
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

    public void reverse() {
        Collections.reverse(vertices);
        Collections.reverse(edges);
        for (SPTEdge e: edges) {
            SPTVertex tmp = e.fromv;
            e.fromv = e.tov;
            e.tov = tmp;
        }
    }
}