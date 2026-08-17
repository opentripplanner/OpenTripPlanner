package org.opentripplanner.ext.carpooling.routing;

import javax.annotation.Nullable;
import org.opentripplanner.astar.model.GraphPath;
import org.opentripplanner.framework.application.OTPRequestTimeoutException;
import org.opentripplanner.street.model.edge.Edge;
import org.opentripplanner.street.model.vertex.Vertex;
import org.opentripplanner.street.search.state.State;

/**
 * Functional interface for street routing.
 */
@FunctionalInterface
public interface CarpoolRouter {
  /**
   * Routes in CAR mode from {@code from} to {@code to}.
   *
   * @return the best path found, or {@code null} when none can be returned: no route exists within
   *         the implementation's search bound, or the search failed unexpectedly.
   * @throws OTPRequestTimeoutException when the request is cancelled. A cancelled search carries no
   *                                   verdict on whether the leg is routable, so it must propagate
   *                                   instead of being reported as a {@code null} return.
   */
  @Nullable
  GraphPath<State, Edge, Vertex> route(Vertex from, Vertex to);
}
