package org.opentripplanner.astar.spi;

import java.util.Collection;

public interface AStarVertex<
  State extends AStarState<State, Edge, Vertex>,
  Edge extends AStarEdge<State, Edge, Vertex>,
  Vertex extends AStarVertex<State, Edge, Vertex>
> {
  /**
   * Get a collection containing all the edges leading from this vertex to other vertices. There is
   * probably some overhead to creating the wrapper ArrayList objects, but this allows filtering and
   * combining edge lists using stock Collection-based methods.
   */
  Collection<Edge> getOutgoing();

  /** Get a collection containing all the edges leading from other vertices to this vertex. */
  Collection<Edge> getIncoming();
}
