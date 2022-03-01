package org.opentripplanner.routing.algorithm.astar.strategies;

import java.util.Set;
import org.opentripplanner.routing.api.request.RoutingRequest;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.graph.Edge;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.routing.spt.ShortestPathTree;

/**
 * Use several strategies in composition with each other, for example by limiting by time and number
 * of stops visited. Only one needs to be skipped in order for {@link
 * ComposingSkipEdgeStrategy#shouldSkipEdge(Set, Set, State, Edge, ShortestPathTree,
 * RoutingRequest)} to return null.
 */
public class ComposingSkipEdgeStrategy implements SkipEdgeStrategy {

    final SkipEdgeStrategy[] strategies;

    public ComposingSkipEdgeStrategy(SkipEdgeStrategy... strategies) {
        this.strategies = strategies;
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
        for (var strategy : strategies) {
            if (strategy.shouldSkipEdge(origins, targets, current, edge, spt, traverseOptions)) {
                return true;
            }
        }
        return false;
    }
}
