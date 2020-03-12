package org.opentripplanner.routing.edgetype.rentedgetype;

import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.core.StateEditor;
import org.opentripplanner.routing.core.TraverseMode;
import org.opentripplanner.routing.core.vehicle_sharing.VehicleDescription;
import org.opentripplanner.routing.graph.Edge;
import org.opentripplanner.routing.graph.Vertex;

import java.util.List;
import java.util.Locale;
import java.util.Map;

public abstract class RentVehicleAnywhereEdge extends Edge {

    public List<VehicleDescription> avaiableVehicles;

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

    abstract boolean isAvailable(long time);

    abstract VehicleDescription getVehicle(long time);

    @Override
    public State traverse(State s0) {
        if (!s0.getOptions().rentingAllowed) {
            return null;
        }

        StateEditor stateEditor = s0.edit(this);

        if (s0.getNonTransitMode().equals(traverseMode())) {
//          We can always finish renting vehicle
//            TODO do we need to check whether we can
            stateEditor.incrementTimeInSeconds(getDropoffTimeInSeconds());
            stateEditor.doneVehicleRenting();
        } else if (s0.getNonTransitMode().equals(TraverseMode.WALK) && isAvailable(s0.getTimeSeconds()) ) {
//          There must be vehicle to in order to rent it
            stateEditor.setCurrentVehicle(getVehicle(s0.getTimeSeconds()));

            stateEditor.incrementTimeInSeconds(getRentTimeInSeconds());
            stateEditor.beginVehicleRenting(traverseMode());
        } else {
            return null;
        }
        return stateEditor.makeState();
    }
}
