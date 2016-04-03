package org.opentripplanner.routing.spt;

import org.opentripplanner.routing.core.RoutingRequest;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.edgetype.StreetEdge;

import java.io.Serializable;
import java.util.Objects;

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
 * 
 * Dominance functions are serializable so that routing requests may passed between machines in different JVMs, for instance
 * in OTPA Cluster.
 */
public abstract class DominanceFunction implements Serializable {
    private static final long serialVersionUID = 1;

    /** 
     * Return true if the first state "defeats" the second state or at least ties with it in terms of suitability. 
     * In the case that they are tied, we still want to return true so that an existing state will kick out a new one.
     * Provide this custom logic in subclasses. You would think this could be static, but in Java for some reason 
     * calling a static function will call the one on the declared type, not the runtime instance type. 
     */
    protected abstract boolean betterOrEqual(State a, State b);

    /**
     * For bike rental, parking, and approaching turn-restricted intersections states are incomparable:
     * they exist on separate planes. The core state dominance logic is wrapped in this public function and only
     * applied when the two states have all these variables in common (are on the same plane).
     */
    public boolean betterOrEqualAndComparable(State a, State b) {

        // States before boarding transit and after riding transit are incomparable.
        // This allows returning transit options even when walking to the destination is the optimal strategy.
        if (a.isEverBoarded() != b.isEverBoarded()) {
            return false;
        }

        // Does one state represent riding a rented bike and the other represent walking before/after rental?
        if (a.isBikeRenting() != b.isBikeRenting()) {
            return false;
        }

        // In case of bike renting, different networks (ie incompatible bikes) are not comparable
        if (a.isBikeRenting()) {
            if (!Objects.equals(a.getBikeRentalNetworks(), b.getBikeRentalNetworks()))
                return false;
        }

        // Does one state represent driving a car and the other represent walking after the car was parked?
        if (a.isCarParked() != b.isCarParked()) {
            return false;
        }

        // Does one state represent riding a bike and the other represent walking after the bike was parked?
        if (a.isBikeParked() != b.isBikeParked()) {
            return false;
        }

        // Are the two states arriving at a vertex from two different directions where turn restrictions apply?
        if (a.backEdge != b.getBackEdge() && (a.backEdge instanceof StreetEdge)) {
            if (! a.getOptions().getRoutingContext().graph.getTurnRestrictions(a.backEdge).isEmpty()) {
                return false;
            }
        }
        
        // These two states are comparable (they are on the same "plane" or "copy" of the graph).
        return betterOrEqual(a, b);
        
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
        @Override
        public boolean betterOrEqual (State a, State b) { return a.weight <= b.weight; }
    }

    /**
     * This approach is more coherent in Analyst when we are extracting travel times from the optimal
     * paths. It also leads to less branching and faster response times when building large shortest path trees.
     */
    public static class EarliestArrival extends DominanceFunction {
        /** Return true if the first state has lower elapsed time than the second state. */
        @Override
        public boolean betterOrEqual (State a, State b) { return a.getElapsedTimeSeconds() <= b.getElapsedTimeSeconds(); }
    }
    
    /**
     * A dominance function that prefers the least walking. This should only be used with walk-only searches because
     * it does not include any functions of time, and once transit is boarded walk distance is constant.
     * 
     * It is used when building stop tree caches for egress from transit stops.
     */
    public static class LeastWalk extends DominanceFunction {

        @Override
        protected boolean betterOrEqual(State a, State b) {
            return a.getWalkDistance() <= b.getWalkDistance(); 
        }

    }

    /** In this implementation the relation is not symmetric. There are sets of mutually co-dominant states. */
    public static class Pareto extends DominanceFunction {

        @Override
        public boolean betterOrEqual (State a, State b) {

            // The key problem in pareto-dominance in OTP is that the elements of the state vector are not orthogonal.
            // When walk distance increases, weight increases. When time increases weight increases.
            // It's easy to get big groups of very similar states that don't represent significantly different outcomes.
            // Our solution to this is to give existing states some slack to dominate new states more easily.
            
            final double EPSILON = 1e-4;
            return (a.getElapsedTimeSeconds() <= (b.getElapsedTimeSeconds() + EPSILON)
                    && a.getWeight() <= (b.getWeight() + EPSILON));
            
        }

    }

}
