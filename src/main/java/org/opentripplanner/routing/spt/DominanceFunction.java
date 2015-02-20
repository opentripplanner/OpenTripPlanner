package org.opentripplanner.routing.spt;

import org.opentripplanner.routing.core.RoutingRequest;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.edgetype.StreetEdge;
import org.opentripplanner.routing.graph.Graph;

/**
 * A class that determines when one search branch prunes another at the same Vertex, and ultimately which solutions
 * are retained. In the general case, one branch does not necessarily win out over the other, i.e. multiple states can
 * coexist at a single Vertex.
 */
public abstract class DominanceFunction {

    /** Return true if the first state is "better" than the second state. */
    public abstract boolean dominates(State a, State b);

    /**
     * Create a new shortest path tree using this function, considering whether it allows co-dominant States.
     * MultiShortestPathTree is the general case -- it will work with both single- and multi-state functions.
     */
     public ShortestPathTree getNewShortestPathTree(RoutingRequest routingRequest) {
        return new MultiStateShortestPathTree(routingRequest, this);
     }

    /** A special case where one state is always better than the other. This allows some optimizations. */
    public static abstract class SingleState extends DominanceFunction {
        @Override
        public ShortestPathTree getNewShortestPathTree(RoutingRequest routingRequest) {
            return new SingleStateShortestPathTree(routingRequest, this);
        }
    }

    public static class MinimumWeight extends DominanceFunction.SingleState {
        /** Return true if the first state has lower weight than the second state. */
        public boolean dominates(State a, State b) { return a.weight < b.weight; }
    }

    /**
     * This approach is more coherent in Analyst when we are extracting travel times from the optimal
     * paths. It also leads to less branching and faster response times when building large shortest path trees.
     */
    public static class EarliestArrival extends DominanceFunction.SingleState {
        /** Return true if the first state has lower elapsed time than the second state. */
        public boolean dominates(State a, State b) { return a.getElapsedTimeSeconds() < b.getElapsedTimeSeconds(); }
    }

    /** In this implementation the relation is not symmetric. There are sets of mutually co-dominant states. */
    public static class Pareto extends DominanceFunction {

        private static final double WALK_DIST_EPSILON = 0.05;
        private static final double WEIGHT_EPSILON = 0.02;
        private static final int WEIGHT_DIFF_MARGIN = 30;
        private static final double TIME_EPSILON = 0.02;
        private static final int TIME_DIFF_MARGIN = 30;

        // You would think this could be static, but in Java for some reason calling a static function will
        // call the one on the declared type, not the instance type.
        public boolean dominates(State a, State b) {

            if (b.weight == 0) {
                return false;
            }
            // Multi-state (bike rental, P+R) - no domination for different states
            if (a.isBikeRenting() != b.isBikeRenting())
                return false;
            if (a.isCarParked() != b.isCarParked())
                return false;
            if (a.isBikeParked() != b.isBikeParked())
                return false;

            Graph graph = a.getOptions().rctx.graph;
            if (a.backEdge != b.getBackEdge() && ((a.backEdge instanceof StreetEdge)
                    && (!graph.getTurnRestrictions(a.backEdge).isEmpty())))
                return false;

            if (a.routeSequenceSubset(b)) {
                // TODO subset is not really the right idea
                return a.weight <= b.weight &&
                        a.getElapsedTimeSeconds() <= b.getElapsedTimeSeconds();
                // && this.getNumBoardings() <= other.getNumBoardings();
            }

            // If returning more than one result from GenericAStar, the search can be very slow
            // unless you replace the following code with:
            // return false;

            boolean walkDistanceIsHopeful = a.walkDistance / b.getWalkDistance() < 1+WALK_DIST_EPSILON;

            double weightRatio = a.weight / b.weight;
            boolean weightIsHopeful = (weightRatio < 1+WEIGHT_EPSILON && a.weight - b.weight < WEIGHT_DIFF_MARGIN);

            double t1 = (double)a.getElapsedTimeSeconds();
            double t2 = (double)b.getElapsedTimeSeconds();
            double timeRatio = t1/t2;
            boolean timeIsHopeful = (timeRatio < 1+TIME_EPSILON) && (t1 - t2 <= TIME_DIFF_MARGIN);

            // only dominate if everything is at least hopeful
            return walkDistanceIsHopeful && weightIsHopeful && timeIsHopeful;
            // return this.weight < other.weight;
        }

    }

}
