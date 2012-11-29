package org.opentripplanner.routing.edgetype;

import org.opentripplanner.routing.core.RoutingRequest;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.core.StateEditor;
import org.opentripplanner.routing.graph.Edge;
import org.opentripplanner.routing.vertextype.TransitStop;

/**
 * Represents a transfer between stops that does not take the street network into account.
 */
public class SimpleTransfer extends Edge {
    
    private static final long serialVersionUID = 1L;

    private int distance;
    
    public SimpleTransfer(TransitStop from, TransitStop to, int distance) {
        super(from, to);
        this.distance = distance;
    }

    @Override
    public State traverse(State s0) {
        // use transfer edges only to transfer 
        // otherwise they are used as shortcuts or break the itinerary generator 
        if ( ! s0.isEverBoarded())
            return null;
        if (s0.getBackEdge() instanceof SimpleTransfer)
            return null;
        RoutingRequest rr = s0.getOptions();
        double walkspeed = rr.getWalkSpeed();
        StateEditor se = s0.edit(this);
        int time = (int) (distance / walkspeed); 
        se.incrementTimeInSeconds(time);
        se.incrementWeight(time * rr.walkReluctance);
        return se.makeState();
    }

    @Override
    public String getName() {
        return "Simple Transfer";
    }

    @Override
    public double weightLowerBound(RoutingRequest rr) {
        int time = (int) (distance / rr.getWalkSpeed()); 
        return (time * rr.walkReluctance);
    }
    
}
