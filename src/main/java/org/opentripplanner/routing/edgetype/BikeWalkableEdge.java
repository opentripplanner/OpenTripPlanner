package org.opentripplanner.routing.edgetype;

import org.opentripplanner.routing.api.request.RoutingRequest;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.core.StateEditor;
import org.opentripplanner.routing.core.TraverseMode;
import org.opentripplanner.routing.graph.Edge;

public interface BikeWalkableEdge {

    default boolean canSwitchToWalkingBike(State state) {
        return state.getNonTransitMode() == TraverseMode.BICYCLE;
    }

    default void switchToWalkingBike(
            RoutingRequest options,
            StateEditor editor,
            boolean includeCostAndTime
    ) {
        editor.setBackWalkingBike(true);
        if (includeCostAndTime) {
            editor.incrementWeight(options.bikeSwitchCost);
            editor.incrementTimeInSeconds(options.bikeSwitchTime);
        }
    }

    default void switchToBiking(RoutingRequest options, StateEditor editor) {
        editor.setBackWalkingBike(false);
        editor.incrementWeight(options.bikeSwitchCost);
        editor.incrementTimeInSeconds(options.bikeSwitchTime);
    }

    default StateEditor createEditorForWalking(State state, Edge edge) {
        StateEditor editor = state.edit(edge);

        if (state.getNonTransitMode() == TraverseMode.CAR) {
            return null;
        }
        else if (state.getNonTransitMode() == TraverseMode.BICYCLE) {
            if (canSwitchToWalkingBike(state)) {
                // If this is the first traversed edge than the bikeSwitch cost doesn't need to be applied
                var shouldIncludeCost = !state.isBackWalkingBike() && hadBackModeSet(state);
                switchToWalkingBike(state.getOptions(), editor, shouldIncludeCost);
            }
            else {
                return null;
            }
        }
        else if (state.getNonTransitMode() != TraverseMode.WALK) {
            return null;
        }

        editor.setBackMode(TraverseMode.WALK);

        return editor;
    }

    default boolean hadBackModeSet(State state) {
        do {
            if (state.getBackMode() != null) {
                return true;
            }
            state = state.getBackState();
        } while (state != null);

        return false;
    }
}
