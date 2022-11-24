package org.opentripplanner.astar.spi;

public interface AStarEdge<
  State extends AStarState<State, Edge, Vertex>,
  Edge extends AStarEdge<State, Edge, Vertex>,
  Vertex extends AStarVertex<State, Edge, Vertex>
> {
  Vertex getFromVertex();

  Vertex getToVertex();

  State traverse(State u);
}
