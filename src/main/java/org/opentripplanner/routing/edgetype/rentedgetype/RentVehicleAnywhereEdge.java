package org.opentripplanner.routing.edgetype.rentedgetype;

import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.core.StateEditor;
import org.opentripplanner.routing.core.vehicle_sharing.VehicleDescription;
import org.opentripplanner.routing.graph.Edge;
import org.opentripplanner.routing.graph.Vertex;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class RentVehicleAnywhereEdge extends Edge {

    private List<VehicleDescription> availableVehicles = new ArrayList<>();

    public RentVehicleAnywhereEdge(Vertex v) {
        super(v, v);
    }

    public List<VehicleDescription> getAvailableVehicles() {
        return availableVehicles;
    }

    @Override
    public String getName() {
        return "Rent vehicle in node " + getToVertex().getName();
    }

    @Override
    public String getName(Locale locale) {
        return "Rent vehicle in node " + getToVertex().getName(locale);
    }

    @Override
    public State traverse(State s0) {
        if (!s0.getOptions().rentingAllowed) {
            return null;
        }

        StateEditor stateEditor = s0.edit(this);
        if (s0.isCurrentlyRentingVehicle()) {
            stateEditor.doneVehicleRenting();
        } else if (!availableVehicles.isEmpty()) {
            // TODO support for many vehicles in one place
            VehicleDescription vehicleDescription = availableVehicles.get(0);
            stateEditor.beginVehicleRenting(vehicleDescription);
        } else {
            return null;
        }
        return stateEditor.makeState();
    }
}
