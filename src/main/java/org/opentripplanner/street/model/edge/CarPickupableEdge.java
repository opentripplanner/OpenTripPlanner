package org.opentripplanner.street.model.edge;

import org.opentripplanner.street.search.state.CarPickupState;
import org.opentripplanner.street.search.state.State;
import org.opentripplanner.street.search.state.StateEditor;

public interface CarPickupableEdge {
  default boolean canPickupAndDrive(State state) {
    return (
      state.getRequest().mode().includesPickup() &&
      state.getCarPickupState() ==
      (
        state.getRequest().arriveBy()
          ? CarPickupState.WALK_FROM_DROP_OFF
          : CarPickupState.WALK_TO_PICKUP
      )
    );
  }

  default boolean canDropOffAfterDriving(State state) {
    return (
      state.getRequest().mode().includesPickup() &&
      state.getCarPickupState() == CarPickupState.IN_CAR
    );
  }

  default void dropOffAfterDriving(State state, StateEditor editor) {
    editor.setCarPickupState(
      state.getRequest().arriveBy()
        ? CarPickupState.WALK_TO_PICKUP
        : CarPickupState.WALK_FROM_DROP_OFF
    );
    editor.incrementTimeInSeconds(state.getPreferences().car().pickupTime());
    editor.incrementWeight(state.getPreferences().car().pickupCost());
  }

  default void driveAfterPickup(State state, StateEditor editor) {
    editor.setCarPickupState(CarPickupState.IN_CAR);
    editor.incrementTimeInSeconds(state.getPreferences().car().pickupTime());
    editor.incrementWeight(state.getPreferences().car().pickupCost());
  }
}
