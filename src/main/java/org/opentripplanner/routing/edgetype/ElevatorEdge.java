package org.opentripplanner.routing.edgetype;

import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.core.StateEditor;
import org.opentripplanner.routing.core.TraverseMode;
import org.opentripplanner.routing.graph.Edge;

/**
 * Marker interface indicating that an edge is part of an elevator.
 * 
 * @author mattwigway
 */
public interface ElevatorEdge extends BikeWalkableEdge {

    default StateEditor createElevatorStateEditor(State s0, Edge edge) {
        if (s0.getNonTransitMode() == TraverseMode.CAR) {
            return s0.edit(edge);
        }

        return createEditorForWalking(s0, edge);
    }
}
