package org.opentripplanner.routing.spt;

import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.edgetype.StreetEdge;
import org.opentripplanner.routing.graph.Graph;

/**
 *
 */
public interface DominanceFunction {

    /** Return true if the first state is better than the second state. */
    public boolean dominates(State a, State b);

    public static class MinimumWeight implements DominanceFunction {

        /** Return true if the first state has lower weight than the second state. */
        public boolean dominates(State a, State b) { return a.weight < b.weight; }

    }

    /**
     * This approach is more coherent in Analyst when we are extracting travel times from the optimal
     * paths. It also leads to less branching and faster response times when building large shortest path trees.
     */
    public static class EarliestArrival implements DominanceFunction {

        /** Return true if the first state has lower elapsed time than the second state. */
        public boolean dominates(State a, State b) { return a.getElapsedTimeSeconds() < b.getElapsedTimeSeconds(); }

    }

    /**
     * In this implementation the relation is not symmetric.
     * There are sets of states where none will dominate the others; they are co-dominant.
     * Therefore this rule should be used with a multi-state shortest path tree.
     */
    public static class ParetoTimeWeight implements DominanceFunction {

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
