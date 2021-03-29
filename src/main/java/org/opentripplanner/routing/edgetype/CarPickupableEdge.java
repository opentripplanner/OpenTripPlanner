package org.opentripplanner.routing.edgetype;

import org.opentripplanner.routing.core.CarPickupState;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.core.StateEditor;

public interface CarPickupableEdge {

    default boolean canPickupAndDrive(State state) {
        return state.getOptions().carPickup
                && state.getCarPickupState() == (
                state.getOptions().arriveBy
                        ? CarPickupState.WALK_FROM_DROP_OFF
                        : CarPickupState.WALK_TO_PICKUP
        );
    }

    default boolean canDropOffAfterDriving(State state) {
        return state.getOptions().carPickup
                && state.getCarPickupState() == CarPickupState.IN_CAR;
    }

    default void dropOffAfterDriving(State state, StateEditor editor) {
        editor.setCarPickupState(state.getOptions().arriveBy
                ? CarPickupState.WALK_TO_PICKUP
                : CarPickupState.WALK_FROM_DROP_OFF);
    }

    default void driveAfterPickup(State state, StateEditor editor) {
        editor.setCarPickupState(CarPickupState.IN_CAR);
    }
}
