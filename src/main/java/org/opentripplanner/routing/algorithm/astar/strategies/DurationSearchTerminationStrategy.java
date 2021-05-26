package org.opentripplanner.routing.algorithm.astar.strategies;

import org.opentripplanner.routing.api.request.RoutingRequest;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.routing.spt.ShortestPathTree;

import java.util.Set;

/**
 * Terminates the search when the specified number of seconds has elapsed. This does not guarantee
 * that we get all relevant results up to the specified duration, as the only criterion we optimize
 * on is generalized cost.
 */
public class DurationSearchTerminationStrategy implements SearchTerminationStrategy {

  private final double durationInSeconds;

  public DurationSearchTerminationStrategy(double durationInSeconds) {
    this.durationInSeconds = durationInSeconds;
  }

  @Override
  public boolean shouldSearchTerminate(
      Set<Vertex> origin,
      Set<Vertex> target,
      State current,
      ShortestPathTree spt,
      RoutingRequest traverseOptions
  ) {
    return current.getElapsedTimeSeconds() > durationInSeconds;
  }
}
