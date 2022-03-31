package org.opentripplanner.routing.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.opentripplanner.routing.api.request.RoutingRequest;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.graph.GraphIndex;
import org.opentripplanner.routing.graph.Vertex;

public class StateEditorTest {

    @Test
    public final void testIncrementTimeInSeconds() {
        Graph graph = new Graph();
        RoutingRequest routingRequest = new RoutingRequest();
        RoutingContext routingContext = new RoutingContext(routingRequest, graph, (Vertex) null, null);
        StateEditor stateEditor = new StateEditor(routingContext, null);

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
        var temporaryVertices = new TemporaryVerticesContainer(graph, request);
        RoutingContext routingContext = new RoutingContext(request, graph, temporaryVertices);
        State state = new State(routingContext);

        state.stateData.vehicleParked = true;
        state.stateData.vehicleRentalState = VehicleRentalState.BEFORE_RENTING;
        state.stateData.currentMode = TraverseMode.WALK;

        StateEditor se = new StateEditor(routingContext, null);
        se.setNonTransitOptionsFromState(state);
        State updatedState = se.makeState();
        assertEquals(TraverseMode.WALK, updatedState.getNonTransitMode());
        assertTrue(updatedState.isVehicleParked());
        assertFalse(updatedState.isRentingVehicle());
        temporaryVertices.close();
    }

    @Test
    public final void testWeightIncrement() {
        Graph graph = new Graph();
        RoutingRequest routingRequest = new RoutingRequest();
        RoutingContext routingContext = new RoutingContext(routingRequest, graph, (Vertex) null, null);
        StateEditor stateEditor = new StateEditor(routingContext, null);

        stateEditor.setTimeSeconds(0);
        stateEditor.incrementWeight(10);

        assertNotNull(stateEditor.makeState());
    }

    @Test
    public final void testNanWeightIncrement() {
        Graph graph = new Graph();
        RoutingRequest routingRequest = new RoutingRequest();
        RoutingContext routingContext = new RoutingContext(routingRequest, graph, (Vertex) null, null);
        StateEditor stateEditor = new StateEditor(routingContext, null);

        stateEditor.setTimeSeconds(0);
        stateEditor.incrementWeight(Double.NaN);

        assertNull(stateEditor.makeState());
    }

    @Test
    public final void testInfinityWeightIncrement() {
        Graph graph = new Graph();
        RoutingRequest routingRequest = new RoutingRequest();
        RoutingContext routingContext = new RoutingContext(routingRequest, graph, (Vertex) null, null);
        StateEditor stateEditor = new StateEditor(routingContext, null);

        stateEditor.setTimeSeconds(0);
        stateEditor.incrementWeight(Double.NEGATIVE_INFINITY);

        assertNull("Infinity weight increment", stateEditor.makeState());
    }
}
