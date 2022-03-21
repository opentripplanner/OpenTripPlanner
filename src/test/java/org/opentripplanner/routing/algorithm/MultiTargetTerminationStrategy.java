package org.opentripplanner.routing.algorithm;

import org.opentripplanner.routing.algorithm.astar.strategies.SearchTerminationStrategy;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.graph.Vertex;

import java.util.HashSet;
import java.util.Set;

/**
 * A termination strategy that terminates after multiple targets have been reached.
 *
 * Useful for implementing a restricted batch search - i.e. doing one-to-many search 
 * without building a full shortest path tree.
 *
 * @author avi
 */
public class MultiTargetTerminationStrategy implements SearchTerminationStrategy {

    private final Set<Vertex> unreachedTargets;
    private final Set<Vertex> reachedTargets;

    public MultiTargetTerminationStrategy(Set<Vertex> targets) {
        unreachedTargets = new HashSet<>(targets);
        reachedTargets = new HashSet<>(targets.size());
    }

    /**
     * Updates the list of reached targets and returns True if all the
     * targets have been reached.
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
