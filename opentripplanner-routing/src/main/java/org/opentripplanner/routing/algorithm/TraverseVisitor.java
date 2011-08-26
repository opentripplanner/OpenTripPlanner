package org.opentripplanner.routing.algorithm;

import org.opentripplanner.routing.core.Edge;
import org.opentripplanner.routing.core.State;

public interface TraverseVisitor {

    /** Called when A* explores an edge */
    void visitEdge(Edge edge, State state);

    /** Called when A* dequeues a vertex */
    void visitVertex(State staet);

}
