package org.opentripplanner.routing.edgetype.rentedgetype;

import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.core.StateEditor;
import org.opentripplanner.routing.core.TraverseMode;
import org.opentripplanner.routing.graph.Edge;
import org.opentripplanner.routing.graph.Vertex;

import java.util.Locale;

public abstract class RentVehicleAnywhereEdge extends Edge {

    protected RentVehicleAnywhereEdge(Vertex v) {
        super(v, v);
    }

    abstract public TraverseMode traverseMode();

    @Override
    public String getName() {
        return "Rent " + traverseMode().name() + " in node " + getToVertex().getName();
    }

    @Override
    public String getName(Locale locale) {
        return "Rent " + traverseMode().name() + " in node " + getToVertex().getName(locale);
    }

    protected abstract int getRentTimeInSeconds();

    protected abstract int getDropoffTimeInSeconds();

    public int available = 0;

    boolean isAvailable(long time) {
        return available > 0;
    }

    @Override
    public State traverse(State s0) {
        StateEditor stateEditor = s0.edit(this);

        if (s0.getNonTransitMode().equals(traverseMode())) {
            stateEditor.incrementTimeInSeconds(getDropoffTimeInSeconds());
            stateEditor.doneVehicleRenting();
        } else if (s0.getNonTransitMode().equals(TraverseMode.WALK) && isAvailable(s0.getTimeSeconds())) {
            stateEditor.incrementTimeInSeconds(getRentTimeInSeconds());
            stateEditor.beginVehicleRenting(traverseMode());
        } else {
            return null;
        }
        return stateEditor.makeState();
    }
}
