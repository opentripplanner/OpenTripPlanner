package org.opentripplanner.street.model.edge;

import org.opentripplanner.routing.api.request.preference.RoutingPreferences;
import org.opentripplanner.street.search.TraverseMode;
import org.opentripplanner.street.search.state.State;
import org.opentripplanner.street.search.state.StateEditor;

public interface BikeWalkableEdge {
  default boolean canSwitchToWalkingBike(State state) {
    return state.currentMode() == TraverseMode.BICYCLE;
  }

  default void switchToWalkingBike(RoutingPreferences preferences, StateEditor editor) {
    // If this is the first traversed edge than the bikeSwitch cost doesn't need to be applied
    var parentState = editor.getBackState();
    var shouldIncludeCost = !parentState.isBackWalkingBike() && hadBackModeSet(parentState);

    editor.setBackWalkingBike(true);
    if (shouldIncludeCost) {
      editor.incrementWeight(preferences.bike().walking().mountDismountCost().toSeconds());
      editor.incrementTimeInMilliseconds(
        preferences.bike().walking().mountDismountTime().toMillis()
      );
    }
  }

  default void switchToBiking(RoutingPreferences preferences, StateEditor editor) {
    var parentState = editor.getBackState();
    var shouldIncludeCost = parentState.isBackWalkingBike();

    editor.setBackWalkingBike(false);
    if (shouldIncludeCost) {
      editor.incrementWeight(preferences.bike().walking().mountDismountCost().toSeconds());
      editor.incrementTimeInMilliseconds(
        preferences.bike().walking().mountDismountTime().toMillis()
      );
    }
  }

  default boolean hadBackModeSet(State state) {
    do {
      if (state.getBackMode() != null) {
        return state.getBackMode().isOnStreetNonTransit();
      }
      state = state.getBackState();
    } while (state != null);

    return false;
  }

  default StateEditor createEditorForDrivingOrWalking(State s0, Edge edge) {
    if (s0.currentMode() == TraverseMode.CAR) {
      return s0.edit(edge);
    }

    return createEditor(s0, edge, TraverseMode.WALK, s0.currentMode() == TraverseMode.BICYCLE);
  }

  default StateEditor createEditorForWalking(State s0, Edge edge) {
    if (s0.currentMode() == TraverseMode.CAR) {
      return null;
    }

    return createEditor(s0, edge, TraverseMode.WALK, s0.currentMode() == TraverseMode.BICYCLE);
  }

  default StateEditor createEditor(State s0, Edge edge, TraverseMode mode, boolean bicycleWalking) {
    StateEditor editor = s0.edit(edge);

    if (bicycleWalking) {
      if (canSwitchToWalkingBike(s0)) {
        switchToWalkingBike(s0.getPreferences(), editor);
      } else {
        return null;
      }
    } else if (mode == TraverseMode.BICYCLE) {
      switchToBiking(s0.getPreferences(), editor);
    }

    editor.setBackMode(mode);

    return editor;
  }
}
