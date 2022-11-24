package org.opentripplanner.astar.spi;

import java.time.Instant;
import org.opentripplanner.routing.core.AStarRequest;

public interface AStarState<
  State extends AStarState<State, Edge, Vertex>,
  Edge extends AStarEdge<State, Edge, Vertex>,
  Vertex extends AStarVertex<State, Edge, Vertex>
> {
  boolean isFinal();

  State getBackState();

  State reverse();

  Edge getBackEdge();

  long getTimeSeconds();

  double getWeight();

  Vertex getVertex();

  State getNextResult();

  long getElapsedTimeSeconds();

  Instant getTime();

  void initBackEdge(Edge originBackEdge);

  AStarRequest getRequest();
}
