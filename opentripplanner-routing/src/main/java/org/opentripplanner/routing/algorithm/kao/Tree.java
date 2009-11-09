package org.opentripplanner.routing.algorithm.kao;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

import org.opentripplanner.routing.core.Edge;
import org.opentripplanner.routing.core.Vertex;

public class Tree {
    HashMap<Vertex, Edge> M;

    Tree() {
        M = new HashMap<Vertex, Edge>();
    }

    Tree(HashMap<Vertex, Edge> M) {
        this.M = M;
    }

    public boolean containsVertex(Vertex vertex) {
        return M.containsKey(vertex);
    }

    public void setParent(Vertex vertex, Edge edge) {
        M.put(vertex, edge);
    }

    public Edge getParent(Vertex vertex) {
        return M.get(vertex);
    }

    public ArrayList<Edge> path(Vertex fin) {
        ArrayList<Edge> ret = new ArrayList<Edge>();

        Edge edge = this.M.get(fin);
        while (edge != null) {
            ret.add(edge);
            edge = M.get(edge.fromv);
        }

        Collections.reverse(ret);

        return ret;
    }

}
