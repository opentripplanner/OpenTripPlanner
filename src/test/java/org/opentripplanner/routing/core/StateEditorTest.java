package org.opentripplanner.routing.core;

import org.junit.Before;
import org.junit.Test;
import org.locationtech.jts.geom.Coordinate;
import org.opentripplanner.routing.core.vehicle_sharing.CarDescription;
import org.opentripplanner.routing.core.vehicle_sharing.FuelType;
import org.opentripplanner.routing.core.vehicle_sharing.Gearbox;
import org.opentripplanner.routing.core.vehicle_sharing.Provider;
import org.opentripplanner.routing.edgetype.rentedgetype.DropoffVehicleEdge;
import org.opentripplanner.routing.edgetype.rentedgetype.RentVehicleEdge;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.impl.StreetVertexIndexServiceImpl;
import org.opentripplanner.routing.vertextype.TemporaryRentVehicleVertex;

import static org.junit.Assert.*;

public class StateEditorTest {

    private static final CarDescription CAR_1 = new CarDescription("1", 0, 0, FuelType.ELECTRIC, Gearbox.AUTOMATIC, new Provider(2, "PANEK"));
    private static final double DELTA = 0.1;

    private State state, rentingState;
    private RoutingRequest request;
    private RentVehicleEdge rentVehicleEdge;
    private DropoffVehicleEdge dropoffVehicleEdge;

    @Before
    public void setUp() {
        Graph graph = new Graph();
        TemporaryRentVehicleVertex v = new TemporaryRentVehicleVertex("id", new Coordinate(1, 2), "name");
        rentVehicleEdge = new RentVehicleEdge(v, CAR_1);
        dropoffVehicleEdge = new DropoffVehicleEdge(v);
        request = new RoutingRequest();
        request.setDummyRoutingContext(graph);
        request.setModes(new TraverseModeSet(TraverseMode.WALK, TraverseMode.CAR));
        request.setStartingMode(TraverseMode.WALK);
        state = new State(v, request);
        StateEditor se = state.edit(rentVehicleEdge);
        se.beginVehicleRenting(CAR_1);
        rentingState = se.makeState();
    }

    @Test
    public void testIncrementTimeInSeconds() {
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
    public void testSetNonTransitOptionsFromState() {
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
        state.stateData.currentTraverseMode = TraverseMode.WALK;

        StateEditor se = new StateEditor(request, null);
        se.setNonTransitOptionsFromState(state);
        State updatedState = se.makeState();
        assertEquals(TraverseMode.WALK, updatedState.getNonTransitMode());
        assertTrue(updatedState.isCarParked());
        assertTrue(updatedState.isBikeParked());
        assertFalse(updatedState.isBikeRenting());
    }

    @Test
    public void shouldAllowRentingVehicles() {
        // given
        StateEditor stateEditor = state.edit(rentVehicleEdge);

        // when
        stateEditor.beginVehicleRenting(CAR_1);
        State next = stateEditor.makeState();

        // then
        assertEquals(TraverseMode.CAR, next.getNonTransitMode());
        assertEquals(CAR_1, next.getCurrentVehicle());
        assertEquals(0, next.distanceTraversedInCurrentVehicle, DELTA);
        assertEquals(state.time + request.routingDelays.getRentingTime(CAR_1) * 1000, next.time);
        assertEquals(state.weight + request.routingDelays.getRentingTime(CAR_1) * request.routingReluctances.getRentingReluctance(), next.weight, DELTA);
    }

    @Test
    public void shouldAllowDroppingOffVehicles() {
        // given
        StateEditor stateEditor = rentingState.edit(dropoffVehicleEdge);

        // when
        stateEditor.doneVehicleRenting();
        State next = stateEditor.makeState();

        // then
        assertEquals(TraverseMode.WALK, next.getNonTransitMode());
        assertNull(next.getCurrentVehicle());
        assertEquals(rentingState.time + request.routingDelays.getDropoffTime(CAR_1) * 1000, next.time);
        assertEquals(rentingState.weight + request.routingDelays.getDropoffTime(CAR_1) * request.routingReluctances.getRentingReluctance(), next.weight, DELTA);
    }

    @Test
    public void shouldAllowReverseRentingVehicles() {
        // given
        StateEditor stateEditor = rentingState.edit(rentVehicleEdge);

        // when
        stateEditor.reversedBeginVehicleRenting();
        State next = stateEditor.makeState();

        // then: we drop off a car, but in renting time
        assertEquals(TraverseMode.WALK, next.getNonTransitMode());
        assertNull(next.getCurrentVehicle());
        assertEquals(rentingState.time + request.routingDelays.getRentingTime(CAR_1) * 1000, next.time);
    }

    @Test
    public void shouldAllowReverseDroppingOffVehicles() {
        // given
        StateEditor stateEditor = state.edit(dropoffVehicleEdge);

        // when
        stateEditor.reversedDoneVehicleRenting(CAR_1);
        State next = stateEditor.makeState();

        // then: we rent a car, but in dropoff time
        assertEquals(TraverseMode.CAR, next.getNonTransitMode());
        assertEquals(CAR_1, next.getCurrentVehicle());
        assertEquals(0, next.distanceTraversedInCurrentVehicle, DELTA);
        assertEquals(state.time + request.routingDelays.getDropoffTime(CAR_1) * 1000, next.time);
    }
}
