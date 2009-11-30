package org.opentripplanner.routing.spt;

import java.util.Collections;
import java.util.ListIterator;
import java.util.Vector;

import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.core.TraverseOptions;
import org.opentripplanner.routing.core.TraverseResult;

public class GraphPath {
    public Vector<SPTVertex> vertices;

    public Vector<SPTEdge> edges;

    public GraphPath() {
        this.vertices = new Vector<SPTVertex>();
        this.edges = new Vector<SPTEdge>();
    }

    public void optimize() {
        State state = vertices.lastElement().state;
        State state0 = vertices.firstElement().state;
        if (state0.getTime() >= state.getTime()) {
            // reversed paths are already optimized, because preferences are asymmetric -- people
            // want to arrive as late as possible, but also want to leave as late as possible.
            return;
        }
        TraverseOptions options = vertices.lastElement().options;
        ListIterator<SPTEdge> iterator = edges.listIterator(vertices.size() - 1);
        while (iterator.hasPrevious()) {
            SPTEdge edge = iterator.previous();
            TraverseResult result = edge.payload.traverseBack(state, options);
            assert (result != null);
            state = result.state;
            edge.fromv.state = state;
        }
    }

    public String toString() {
        return vertices.toString();
    }

    public void reverse() {
        Collections.reverse(vertices);
        Collections.reverse(edges);
        for (SPTEdge e : edges) {
            SPTVertex tmp = e.fromv;
            e.fromv = e.tov;
            e.tov = tmp;
        }
    }
}