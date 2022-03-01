package org.opentripplanner.routing.algorithm.astar.strategies;

import java.util.Set;
import org.opentripplanner.routing.api.request.RoutingRequest;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.graph.Edge;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.routing.spt.ShortestPathTree;

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
