package org.opentripplanner.astar.spi;

import org.opentripplanner.astar.model.Edge;
import org.opentripplanner.routing.core.State;

public interface TraverseVisitor {
  /** Called when A* explores an edge */
  void visitEdge(Edge edge);

  /** Called when A* dequeues a vertex */
  void visitVertex(State state);

  /** Called when A* enqueues a vertex */
  void visitEnqueue();
}
