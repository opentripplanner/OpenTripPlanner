package org.opentripplanner.routing.edgetype;

import java.util.Set;

import org.opentripplanner.routing.api.request.RoutingRequest;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.graph.Vertex;

/**
 * Dropping off a rented bike edge.
 * 
 * Cost is the time to dropoff a bike.
 * 
 * @author laurent
 * 
 */
public class RentABikeOffEdge extends RentABikeAbstractEdge {

    private static final long serialVersionUID = 1L;

    public RentABikeOffEdge(Vertex from, Vertex to, Set<String> networks) {
        super(from, to, networks);
    }

    @Override
    public State traverse(State s0) {
        RoutingRequest options = s0.getOptions();
        if (options.arriveBy) {
            return super.traverseRent(s0);
        } else {
            return super.traverseDropoff(s0);
        }
    }

    public boolean equals(Object o) {
        if (o instanceof RentABikeOffEdge) {
            RentABikeOffEdge other = (RentABikeOffEdge) o;
            return other.getFromVertex().equals(fromv) && other.getToVertex().equals(tov);
        }
        return false;
    }

    public String toString() {
        return "RentABikeOffEdge(" + fromv + " -> " + tov + ")";
    }
}
