package org.opentripplanner.routing.algorithm.strategies;

import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.core.RoutingRequest;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.routing.spt.ShortestPathTree;

/**
 *
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
    public boolean shouldSearchTerminate(Vertex origin, Vertex target, State current,
                                         ShortestPathTree spt, RoutingRequest traverseOptions);
}
