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
    
    private int distance;
    
    public SimpleTransfer(TransitStop from, TransitStop to, int distance) {
        super(from, to);
        this.distance = distance;
    }

    @Override
    public State traverse(State s0) {
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

    
    
}
