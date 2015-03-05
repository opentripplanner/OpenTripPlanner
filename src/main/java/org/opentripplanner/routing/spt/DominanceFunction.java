package org.opentripplanner.routing.spt;

import org.opentripplanner.routing.core.RoutingRequest;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.edgetype.StreetEdge;

/**
 * A class that determines when one search branch prunes another at the same Vertex, and ultimately which solutions
 * are retained. In the general case, one branch does not necessarily win out over the other, i.e. multiple states can
 * coexist at a single Vertex.
 * 
 * Even functions where one state always wins (least weight, fastest travel time) are applied within a multi-state
 * shortest path tree because bike rental, car or bike parking, and turn restrictions all require multiple incomparable 
 * states at the same vertex. These need the graph to be "replicated" into separate layers, which is achieved by 
 * applying the main dominance logic (lowest weight, lowest cost, Pareto) conditionally, only when the two states
 * have identical bike/car/turn direction status.
 */
public abstract class DominanceFunction {

    /** Return true if the first state "defeats" the second state. Provide this custom logic in subclasses. */
    protected abstract boolean dominates0 (State a, State b);

    /**
     * For bike rental, parking, and approaching turn-restricted intersections states are incomparable:
     * they exist on separate planes. The core state dominance logic is wrapped in this public function and only
     * applied when the two states have all these variables in common (are on the same plane).
     */
    public boolean dominates(State a, State b) {

        // Does one state represent riding a rented bike and the other represent walking before/after rental?
        if (a.isBikeRenting() != b.isBikeRenting()) {
            return false;
        }

        // Does one state represent driving a car and the other represent walking after the car was parked?
        if (a.isCarParked() != b.isCarParked()) {
            return false;
        }
        
        // Are the two states arriving at a vertex from two different directions where turn restrictions apply?
        if (a.backEdge != b.getBackEdge() && (a.backEdge instanceof StreetEdge)) {
            if (! a.getOptions().getRoutingContext().graph.getTurnRestrictions(a.backEdge).isEmpty()) {
                return false;
            }
        }
        
        // These two states are comparable (they are on the same "plane" or "copy" of the graph).
        return dominates0 (a, b);
        
    }
    
    /**
     * Create a new shortest path tree using this function, considering whether it allows co-dominant States.
     * MultiShortestPathTree is the general case -- it will work with both single- and multi-state functions.
     */
     public ShortestPathTree getNewShortestPathTree(RoutingRequest routingRequest) {
        return new ShortestPathTree(routingRequest, this);
     }

    public static class MinimumWeight extends DominanceFunction {
        /** Return true if the first state has lower weight than the second state. */
        public boolean dominates0 (State a, State b) { return a.weight < b.weight; }
    }

    /**
     * This approach is more coherent in Analyst when we are extracting travel times from the optimal
     * paths. It also leads to less branching and faster response times when building large shortest path trees.
     */
    public static class EarliestArrival extends DominanceFunction {
        /** Return true if the first state has lower elapsed time than the second state. */
        public boolean dominates0 (State a, State b) { return a.getElapsedTimeSeconds() < b.getElapsedTimeSeconds(); }
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
        public boolean dominates0 (State a, State b) {

            // The key problem in pareto-dominance in OTP is that the elements of the state vector are not orthogonal.
            // When walk distance increases, weight increases. When time increases weight increases.
            // It's easy to get big groups of very similar states that don't represent significantly different outcomes.
            // So this probably all deserves to be rethought.
            
            boolean walkDistanceIsHopeful = a.walkDistance / b.getWalkDistance() < 1+WALK_DIST_EPSILON;

            double weightRatio = a.weight / b.weight;
            boolean weightIsHopeful = (weightRatio < 1+WEIGHT_EPSILON && a.weight - b.weight < WEIGHT_DIFF_MARGIN);

            double t1 = (double)a.getElapsedTimeSeconds();
            double t2 = (double)b.getElapsedTimeSeconds();
            double timeRatio = t1/t2;
            boolean timeIsHopeful = (timeRatio < 1+TIME_EPSILON) && (t1 - t2 <= TIME_DIFF_MARGIN);

            // only dominate if everything is at least hopeful
            return walkDistanceIsHopeful && weightIsHopeful && timeIsHopeful;

        }

    }

}
