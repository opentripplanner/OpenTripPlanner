package org.opentripplanner.routing.edgetype.rentedgetype;

import org.junit.Before;
import org.junit.Test;
import org.opentripplanner.routing.core.*;
import org.opentripplanner.routing.core.vehicle_sharing.*;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.vertextype.IntersectionVertex;

import java.util.List;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public class DropoffVehicleEdgeTest {

    private static final CarDescription CAR_1 = new CarDescription("1", 0, 0, FuelType.ELECTRIC, Gearbox.AUTOMATIC, new Provider(2, "PANEK"));

    private DropoffVehicleEdge edge;
    private RoutingRequest request;
    private State rentingState;

    @Before
    public void setUp() {
        Graph graph = new Graph();
        IntersectionVertex v = new IntersectionVertex(graph, "v_name", 0, 0);
        edge = new DropoffVehicleEdge(v);
        request = new RoutingRequest();
        request.setDummyRoutingContext(graph);
        request.setModes(new TraverseModeSet(TraverseMode.WALK, TraverseMode.CAR));
        request.setStartingMode(TraverseMode.WALK);
        State state = new State(v, request);
        StateEditor se = state.edit(edge);
        se.beginVehicleRenting(CAR_1);
        rentingState = se.makeState();
    }

    @Test
    public void shouldNotAllowToDropoffVehicleOutsideParkingZone() {
        // given
        List<SingleParkingZone> singleParkingZone = singletonList(new SingleParkingZone(2, VehicleType.CAR));
        edge.updateParkingZones(singleParkingZone, emptyList());

        // when
        State traversed = edge.traverse(rentingState);

        // then
        assertNull(traversed);
    }

    @Test
    public void shouldAllowToDropoffVehicleInsideParkingZone() {
        // given
        request.rentingAllowed = true;
        List<SingleParkingZone> singleParkingZone = singletonList(new SingleParkingZone(2, VehicleType.CAR));
        edge.updateParkingZones(singleParkingZone, singleParkingZone);

        // when
        State traversed = edge.traverse(rentingState);

        // then
        assertNotNull(traversed);
    }

    @Test
    public void shouldAllowToDropoffVehicleWhenParkingZonesDontApplyToIt() {
        // given
        request.rentingAllowed = true;
        List<SingleParkingZone> singleParkingZone = singletonList(new SingleParkingZone(2, VehicleType.MOTORBIKE));
        edge.updateParkingZones(singleParkingZone, emptyList());

        // when
        State traversed = edge.traverse(rentingState);

        // then
        assertNotNull(traversed);
    }
}
