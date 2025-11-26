package org.opentripplanner.ext.carpooling.routing;

import org.opentripplanner.astar.model.GraphPath;
import org.opentripplanner.model.GenericLocation;
import org.opentripplanner.routing.linking.LinkingContext;
import org.opentripplanner.street.model.edge.Edge;
import org.opentripplanner.street.model.vertex.Vertex;
import org.opentripplanner.street.search.state.State;

/**
 * Functional interface for street routing.
 */
@FunctionalInterface
public interface RoutingFunction {
  GraphPath<State, Edge, Vertex> route(
    GenericLocation from,
    GenericLocation to,
    LinkingContext linkingContext
  );
}
