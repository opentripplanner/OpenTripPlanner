package org.opentripplanner.routing.algorithm;

import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.graph.Edge;

public interface TraverseVisitor {

    /** Called when A* explores an edge */
    void visitEdge(Edge edge, State state);

    /** Called when A* dequeues a vertex */
    void visitVertex(State state);

    /** Called when A* enqueues a vertex */
    void visitEnqueue(State state);

}
