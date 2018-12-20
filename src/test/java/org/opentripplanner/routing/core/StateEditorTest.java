package org.opentripplanner.routing.core;

import org.junit.Test;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.impl.StreetVertexIndexServiceImpl;

import static org.junit.Assert.assertEquals;

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
        graph.streetIndex = new StreetVertexIndexServiceImpl(graph);
        request.rctx = new RoutingContext(request, graph);
        State state = new State(request);

        state.stateData.carParked = true;
        state.stateData.bikeParked = true;
        state.stateData.usingRentedBike = false;
        state.stateData.nonTransitMode = TraverseMode.WALK;

        StateEditor se = new StateEditor(request, null);
        se.setNonTransitOptionsFromState(state);
        State updatedState = se.makeState();
        assertEquals(TraverseMode.WALK, updatedState.getNonTransitMode());
        assertEquals(true, updatedState.isCarParked());
        assertEquals(true, updatedState.isBikeParked());
        assertEquals(false, updatedState.isBikeRenting());
    }
}
