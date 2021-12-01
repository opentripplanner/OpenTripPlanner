package org.opentripplanner.routing.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.opentripplanner.routing.api.request.RoutingRequest;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.graph.GraphIndex;

public class StateEditorTest {

    @Test
    public final void testIncrementTimeInSeconds() {
        RoutingRequest routingRequest = new RoutingRequest();
        StateEditor stateEditor = new StateEditor(routingRequest, null);

        stateEditor.setTimeSeconds(0);
        stateEditor.incrementTimeInSeconds(999999999);

        assertEquals(999999999, stateEditor.child.getTimeSeconds());
    }

    /**
     * Test update of non transit options.
     */
    @Test
    public final void testSetNonTransitOptionsFromState(){
        RoutingRequest request = new RoutingRequest();
        request.setMode(TraverseMode.CAR);
        request.parkAndRide = true;
        Graph graph = new Graph();
        graph.index = new GraphIndex(graph);
        request.rctx = new RoutingContext(request, graph);
        State state = new State(request);

        state.stateData.vehicleParked = true;
        state.stateData.vehicleRentalState = VehicleRentalState.BEFORE_RENTING;
        state.stateData.currentMode = TraverseMode.WALK;

        StateEditor se = new StateEditor(request, null);
        se.setNonTransitOptionsFromState(state);
        State updatedState = se.makeState();
        assertEquals(TraverseMode.WALK, updatedState.getNonTransitMode());
        assertTrue(updatedState.isVehicleParked());
        assertFalse(updatedState.isRentingVehicle());
    }
}
