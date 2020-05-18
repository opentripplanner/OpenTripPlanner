package org.opentripplanner.routing.edgetype;

import java.util.Set;

import org.opentripplanner.routing.api.request.RoutingRequest;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.vertextype.BikeRentalStationVertex;

/**
 * Renting a bike edge.
 * 
 * Cost is the time to pickup a bike plus "inconvenience of renting".
 * 
 * @author laurent
 * 
 */
public class RentABikeOnEdge extends RentABikeAbstractEdge {

    private static final long serialVersionUID = 1L;

    public RentABikeOnEdge(BikeRentalStationVertex from, BikeRentalStationVertex to, Set<String> networks) {
        super(from, to, networks);
    }

    @Override
    public State traverse(State s0) {
        RoutingRequest options = s0.getOptions();
        if (options.arriveBy) {
            return super.traverseDropoff(s0);
        } else {
            return super.traverseRent(s0);
        }
    }

    public boolean equals(Object o) {
        if (o instanceof RentABikeOnEdge) {
            RentABikeOnEdge other = (RentABikeOnEdge) o;
            return other.getFromVertex().equals(fromv) && other.getToVertex().equals(tov);
        }
        return false;
    }

    public String toString() {
        return "RentABikeOnEdge(" + fromv + " -> " + tov + ")";
    }
}
