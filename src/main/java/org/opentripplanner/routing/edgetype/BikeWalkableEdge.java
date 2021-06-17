package org.opentripplanner.routing.edgetype;

import org.opentripplanner.routing.api.request.RoutingRequest;
import org.opentripplanner.routing.core.StateEditor;

public interface BikeWalkableEdge {

    default void switchToWalkingBike(RoutingRequest options, StateEditor editor, boolean includeCostAndTime) {
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
}
