package org.opentripplanner.routing.algorithm.astar.strategies;

import org.opentripplanner.routing.api.request.RoutingRequest;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.graph.Edge;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.routing.spt.ShortestPathTree;

import java.util.Set;

/**
 * Strategy interface to provide additional logic to decide if a given edge should not be considered
 * for traversal.
 * 
 * @author bdferris
 * 
 */
public interface SkipEdgeStrategy {

    /**
     * 
     * @param origins the origin vertices
     * @param targets the target vertices, may be null in an undirected search
     * @param current the current vertex
     * @param edge the current edge to potentially be skipped
     * @param spt the shortest path tree
     * @param traverseOptions the traverse options
     * @return true if the given edge should not be considered for traversal
     */
    boolean shouldSkipEdge(
        Set<Vertex> origins,
        Set<Vertex> targets,
        State current,
        Edge edge,
        ShortestPathTree spt,
        RoutingRequest traverseOptions
    );
}
