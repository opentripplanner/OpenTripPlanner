package org.opentripplanner.routing.edgetype.rentedgetype;

import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.core.StateEditor;
import org.opentripplanner.routing.core.vehicle_sharing.VehicleDescription;
import org.opentripplanner.routing.edgetype.TemporaryEdge;
import org.opentripplanner.routing.vertextype.TemporaryRentVehicleVertex;

import java.util.Locale;

/**
 * This edge allows us to rent vehicle (or leave current vehicle and rent new one).
 * This edge is a loop on {@link TemporaryRentVehicleVertex} which, when traversed, changes our current traverse mode,
 * but leaves us in the same location.
 */
public class RentVehicleEdge extends EdgeWithParkingZones implements TemporaryEdge {

    private final VehicleDescription vehicle;

    public RentVehicleEdge(TemporaryRentVehicleVertex v, VehicleDescription vehicle) {
        super(v, v);
        this.vehicle = vehicle;
    }

    @Override
    public String getName() {
        return "Rent vehicle " + vehicle;
    }

    @Override
    public String getName(Locale locale) {
        return "Rent vehicle " + vehicle;
    }

    @Override
    public State traverse(State state) {
        if (!state.getOptions().rentingAllowed) {
            return null;
        }
        if (state.getOptions().vehicleValidator.isValid(vehicle)) {
            if (state.isCurrentlyRentingVehicle()) {
                return trySwitchVehicles(state, vehicle);
            } else {
                return beginVehicleRenting(state, vehicle);
            }
        }
        return null;
    }

    // TODO AdamWiktor VMP-4 reverse-optimize

    private State trySwitchVehicles(State state, VehicleDescription vehicle) {
        if (!canDropoffVehicleHere(state.getCurrentVehicle())) {
            return null;
        }
        StateEditor stateEditor = state.edit(this);
        stateEditor.doneVehicleRenting();
        stateEditor.beginVehicleRenting(vehicle);
        return stateEditor.makeState();
    }

    private State beginVehicleRenting(State state, VehicleDescription vehicle) {
        StateEditor next = state.edit(this);
        next.beginVehicleRenting(vehicle);
        return next.makeState();
    }

    public VehicleDescription getVehicle() {
        return vehicle;
    }
}
