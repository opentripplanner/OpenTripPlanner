package org.opentripplanner.ext.carpooling.routing;

import org.opentripplanner.astar.model.GraphPath;
import org.opentripplanner.street.model.edge.Edge;
import org.opentripplanner.street.model.vertex.Vertex;
import org.opentripplanner.street.search.state.State;

/**
 * Functional interface for street routing.
 */
@FunctionalInterface
public interface CarpoolRouter {
  GraphPath<State, Edge, Vertex> route(Vertex from, Vertex to);
}
