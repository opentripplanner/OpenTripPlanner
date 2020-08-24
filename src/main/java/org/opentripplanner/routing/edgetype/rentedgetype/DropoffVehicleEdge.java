package org.opentripplanner.routing.edgetype.rentedgetype;

import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.core.StateEditor;
import org.opentripplanner.routing.core.vehicle_sharing.VehicleDescription;
import org.opentripplanner.routing.graph.Vertex;

import java.util.Locale;

/**
 * This edge is used to allow dropping off rented vehicles at any street vertex we want.
 */
public class DropoffVehicleEdge extends EdgeWithParkingZones {

    public DropoffVehicleEdge(Vertex v) {
        super(v, v);
    }

    @Override
    public String getName() {
        return "Drop off vehicle in node " + tov.getName();
    }

    @Override
    public String getName(Locale locale) {
        return "Drop off vehicle in node " + tov.getName(locale);
    }

    @Override
    public State traverse(State state) {
        if (state.isCurrentlyRentingVehicle() && canDropoffVehicleHere(state.getCurrentVehicle())) {
            return doneVehicleRenting(state);
        } else {
            return null;
        }
    }

    public State reversedTraverseDoneRenting(State state, VehicleDescription vehicle) {
        StateEditor next = state.edit(this);
        next.reversedDoneVehicleRenting(vehicle);
        return next.makeState();
    }

    private State doneVehicleRenting(State state) {
        StateEditor stateEditor = state.edit(this);
        stateEditor.doneVehicleRenting();
        return stateEditor.makeState();
    }
}
