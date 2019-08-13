package org.opentripplanner.routing.flex;

import org.opentripplanner.routing.algorithm.GenericDijkstra;
import org.opentripplanner.routing.algorithm.TraverseVisitor;
import org.opentripplanner.routing.algorithm.strategies.SearchTerminationStrategy;
import org.opentripplanner.routing.algorithm.strategies.TrivialRemainingWeightHeuristic;
import org.opentripplanner.routing.core.RoutingRequest;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.core.TraverseMode;
import org.opentripplanner.routing.core.TraverseModeSet;
import org.opentripplanner.routing.edgetype.StreetEdge;
import org.opentripplanner.routing.edgetype.TemporaryEdge;
import org.opentripplanner.routing.graph.Edge;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.routing.spt.ShortestPathTree;

public class CarPermissionSearch {

    private RoutingRequest opt;

    public CarPermissionSearch(RoutingRequest opt, boolean arriveBy) {
        this.opt = opt.clone();
        this.opt.setMode(TraverseMode.WALK);
        this.opt.setArriveBy(arriveBy);
    }

    public Vertex findVertexWithPermission(Vertex vertex, TraverseMode mode) {
        SearchStrategy strategy = new SearchStrategy(mode);
        GenericDijkstra gd = new GenericDijkstra(opt);
        gd.setHeuristic(new TrivialRemainingWeightHeuristic());
        gd.traverseVisitor = strategy;
        gd.setSearchTerminationStrategy(strategy);
        gd.getShortestPathTree(new State(vertex, opt));
        return strategy.getVertex();
    }

    private static class SearchStrategy implements SearchTerminationStrategy, TraverseVisitor {

        private final TraverseMode mode;

        private Vertex vertex = null;

        private boolean firstStreetEdge = true;

        public SearchStrategy(TraverseMode mode) {
            this.mode = mode;
        }

        // TraverseVisitor

        @Override
        public void visitEdge(Edge edge, State state) {
            boolean arriveBy = state.getOptions().arriveBy;
            if (vertex == null && edge instanceof StreetEdge && !(edge instanceof TemporaryEdge)) {
                if (((StreetEdge) edge).canTraverse(new TraverseModeSet(mode))) {
                    if (firstStreetEdge) {
                        vertex = arriveBy ? state.getOptions().rctx.toVertex : state.getOptions().rctx.fromVertex;
                    } else {
                        vertex = arriveBy ? edge.getToVertex() : edge.getFromVertex();
                    }
                }
                firstStreetEdge = false;
            }
        }

        @Override
        public void visitVertex(State state) {
        }

        @Override
        public void visitEnqueue(State state) {
        }

        // TerminationStrategy
        @Override
        public boolean shouldSearchTerminate(Vertex origin, Vertex target, State current, ShortestPathTree spt, RoutingRequest traverseOptions) {
            return vertex != null;
        }

        public Vertex getVertex() {
            return vertex;
        }
    }
}
