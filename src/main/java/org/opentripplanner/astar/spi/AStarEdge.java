package org.opentripplanner.astar.spi;

import javax.annotation.Nonnull;

public interface AStarEdge<
  State extends AStarState<State, Edge, Vertex>,
  Edge extends AStarEdge<State, Edge, Vertex>,
  Vertex extends AStarVertex<State, Edge, Vertex>
> {
  Vertex getFromVertex();

  Vertex getToVertex();

  @Nonnull
  State[] traverse(State u);
}
