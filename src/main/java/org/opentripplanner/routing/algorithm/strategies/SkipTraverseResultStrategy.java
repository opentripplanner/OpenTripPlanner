package org.opentripplanner.routing.algorithm.strategies;

import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.core.RoutingRequest;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.routing.spt.ShortestPathTree;

/**
 * Strategy interface to provide additional logic to decide if a given traverse result should not be
 * considered further.
 * 
 * @author bdferris
 * 
 */
public interface SkipTraverseResultStrategy {

    /**
     * 
     * @param origin the origin vertex
     * @param target the target vertex, may be null in an undirected search
     * @param parent the parent shortest-path-tree vertex
     * @param traverseResult the current traverse result to consider for skipping
     * @param spt the shortest path tree
     * @param traverseOptions the current traverse options
     * @param remainingWeightEstimate the remaining weight estimate from the heuristic (or -1 if no heuristic)
     * @return true if the given traverse result should not be considered further
     */
    public boolean shouldSkipTraversalResult(Vertex origin, Vertex target, State parent,
            State current, ShortestPathTree spt, RoutingRequest traverseOptions);
}
