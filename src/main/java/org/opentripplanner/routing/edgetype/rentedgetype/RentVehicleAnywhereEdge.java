package org.opentripplanner.routing.edgetype.rentedgetype;

import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.core.StateEditor;
import org.opentripplanner.routing.core.TraverseMode;
import org.opentripplanner.routing.graph.Edge;
import org.opentripplanner.routing.graph.Vertex;

import java.util.Locale;

public abstract class RentVehicleAnywhereEdge extends Edge {
    abstract public TraverseMode traverseMode();

    @Override
    public String getName() {
        return "Rent vehicle "+traverseMode();
    }

    @Override
    public String getName(Locale locale) {
        return null; // No idea what to do with it
    }

    protected abstract int getRentTimeInSeconds();

    protected abstract int getDropoffTimeInSeconds();

    protected int getRentFee(Vertex v) {
        return 0;
    }

    public boolean isAvaiable = true;

    boolean isAvaiable(long time) {
        return isAvaiable;
    }


    @Override
    public State traverse(State s0) {
        StateEditor stateEditor = s0.edit(this);

        if (s0.getNonTransitMode().equals(traverseMode())) {
            stateEditor.incrementTimeInSeconds(getDropoffTimeInSeconds());
            stateEditor.beginVehicleRenting(traverseMode());
        } else if (s0.getNonTransitMode().equals(TraverseMode.WALK) && isAvaiable(s0.getTimeSeconds())) {
            stateEditor.incrementTimeInSeconds(getRentTimeInSeconds());
            stateEditor.doneVehicleRenting();
        } else {
            return null;
        }

        return stateEditor.makeState();
    }

    protected RentVehicleAnywhereEdge(Vertex v1, Vertex v2) {
        super(v1, v2);
    }
}
