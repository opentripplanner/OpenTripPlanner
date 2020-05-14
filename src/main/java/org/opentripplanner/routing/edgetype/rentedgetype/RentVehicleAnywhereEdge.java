package org.opentripplanner.routing.edgetype.rentedgetype;

import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.core.StateEditor;
import org.opentripplanner.routing.core.vehicle_sharing.VehicleDescription;
import org.opentripplanner.routing.graph.Edge;
import org.opentripplanner.routing.graph.Vertex;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

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

        if (s0.isCurrentlyRentingVehicle()) {
            return doneVehicleRenting(s0);
        } else {
            List<VehicleDescription> rentableVehicles = availableVehicles.stream()
                    .filter(v -> s0.getOptions().vehiclesAllowedToRent.isRentable(v))
                    .collect(Collectors.toList());

            State previous = null;
            for (VehicleDescription rentableVehicle : rentableVehicles) {
                previous = beginVehicleRenting(s0, rentableVehicle).addToExistingResultChain(previous);
            }
            return previous;
        }
    }

    public State reversedTraverseDoneRenting(State s0, VehicleDescription vehicle) {
        StateEditor next = s0.edit(this);
        next.reversedDoneVehicleRenting(vehicle);
        return next.makeState();
    }

    public State reversedTraverseBeginRenting(State s0) {
        StateEditor next = s0.edit(this);
        next.reversedBeginVehicleRenting();
        return next.makeState();
    }

    public State beginVehicleRenting(State s0, VehicleDescription vehicle) {
        StateEditor next = s0.edit(this);
        next.beginVehicleRenting(vehicle);
        return next.makeState();
    }

    public State doneVehicleRenting(State s0) {
        StateEditor stateEditor = s0.edit(this);
        stateEditor.doneVehicleRenting();
        return stateEditor.makeState();
    }
}
