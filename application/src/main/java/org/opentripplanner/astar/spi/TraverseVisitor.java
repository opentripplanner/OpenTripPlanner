package org.opentripplanner.astar.spi;

public interface TraverseVisitor<
  State extends AStarState<State, Edge, ?>, Edge extends AStarEdge<State, Edge, ?>
> {
  /** Called when A* explores an edge */
  void visitEdge(Edge edge);

  /** Called when A* dequeues a vertex */
  void visitVertex(State state);

  /** Called when A* enqueues a vertex */
  void visitEnqueue();
}
