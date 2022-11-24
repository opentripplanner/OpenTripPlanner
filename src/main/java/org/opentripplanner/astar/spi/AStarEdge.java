package org.opentripplanner.astar.spi;

import org.locationtech.jts.geom.LineString;

public interface AStarEdge<
  State extends AStarState<State, Edge, Vertex>,
  Edge extends AStarEdge<State, Edge, Vertex>,
  Vertex extends AStarVertex<State, Edge, Vertex>
> {
  Vertex getFromVertex();

  Vertex getToVertex();

  LineString getGeometry();

  State traverse(State u);

  double getDistanceMeters();
}
