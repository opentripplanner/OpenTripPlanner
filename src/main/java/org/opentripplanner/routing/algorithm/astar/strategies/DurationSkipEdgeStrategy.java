package org.opentripplanner.routing.algorithm.astar.strategies;

import org.opentripplanner.routing.api.request.RoutingRequest;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.graph.Edge;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.routing.spt.ShortestPathTree;

import java.util.Set;

/**
 * Skips edges when the specified number of seconds has elapsed. This does not guarantee
 * that we get all relevant results up to the specified duration, as the only criterion we optimize
 * on is generalized cost.
 */
public class DurationSkipEdgeStrategy implements SkipEdgeStrategy {

  private final double durationInSeconds;

  public DurationSkipEdgeStrategy(double durationInSeconds) {
    this.durationInSeconds = durationInSeconds;
  }

  @Override
  public boolean shouldSkipEdge(
      Set<Vertex> origins,
      Set<Vertex> targets,
      State current,
      Edge edge, 
      ShortestPathTree spt,
      RoutingRequest traverseOptions
  ) {
    return current.getElapsedTimeSeconds() > durationInSeconds;
  }
}
