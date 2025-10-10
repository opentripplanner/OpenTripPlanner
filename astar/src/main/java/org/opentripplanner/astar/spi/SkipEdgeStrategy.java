package org.opentripplanner.astar.spi;

/**
 * Strategy interface to provide additional logic to decide if a given edge should not be considered
 * for traversal. This can also be used to limit the search, for example by duration or distance.
 * For limiting purposes it is preferable to {@link SearchTerminationStrategy} as it only terminates
 * the current path, but continues searching along other paths until they also are terminated.
 *
 * @author bdferris
 */
public interface SkipEdgeStrategy<
  State extends AStarState<State, Edge, ?>, Edge extends AStarEdge<State, Edge, ?>
> {
  /**
   * @param current the current vertex
   * @param edge    the current edge to potentially be skipped
   * @return true if the given edge should not be considered for traversal
   */
  boolean shouldSkipEdge(State current, Edge edge);
}
