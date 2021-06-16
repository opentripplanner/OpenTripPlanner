package org.opentripplanner.routing.algorithm.astar.strategies;

import org.opentripplanner.routing.api.request.RoutingRequest;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.routing.spt.ShortestPathTree;

import java.util.Set;

/**
 * Immediately terminates the search once the condition has been reached. This can be useful for
 * checking that the required number of targets have been reached, but not for limiting searches
 * but distance or duration, as it will not continue searching along other paths once the condition
 * has been met.
 */
public interface SearchTerminationStrategy {

    /**
     * @param origin the origin vertex
     * @param target the target vertex, may be null in an undirected search
     * @param current the current shortest path tree vertex
     * @param spt the current shortest path tree
     * @param traverseOptions the traverse options
     * @return true if the specified search should be terminated
     */
    public boolean shouldSearchTerminate(Set<Vertex> origin, Set<Vertex> target, State current,
        ShortestPathTree spt, RoutingRequest traverseOptions);
}
