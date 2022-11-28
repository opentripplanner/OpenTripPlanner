package org.opentripplanner.routing.algorithm;

import java.util.HashSet;
import java.util.Set;
import org.opentripplanner.astar.spi.SearchTerminationStrategy;
import org.opentripplanner.street.model.vertex.Vertex;
import org.opentripplanner.street.search.state.State;

/**
 * A termination strategy that terminates after multiple targets have been reached.
 * <p>
 * Useful for implementing a restricted batch search - i.e. doing one-to-many search without
 * building a full shortest path tree.
 *
 * @author avi
 */
public class MultiTargetTerminationStrategy implements SearchTerminationStrategy<State> {

  private final Set<Vertex> unreachedTargets;
  private final Set<Vertex> reachedTargets;

  public MultiTargetTerminationStrategy(Set<Vertex> targets) {
    unreachedTargets = new HashSet<>(targets);
    reachedTargets = new HashSet<>(targets.size());
  }

  /**
   * Updates the list of reached targets and returns True if all the targets have been reached.
   */
  @Override
  public boolean shouldSearchTerminate(State current) {
    Vertex currentVertex = current.getVertex();

    // TODO(flamholz): update this to handle vertices that are not in the graph
    // but rather along edges in the graph.
    if (unreachedTargets.contains(currentVertex)) {
      unreachedTargets.remove(currentVertex);
      reachedTargets.add(currentVertex);
    }
    return unreachedTargets.size() == 0;
  }
}
