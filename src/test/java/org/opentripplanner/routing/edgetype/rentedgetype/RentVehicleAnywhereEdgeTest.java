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
import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class RentVehicleAnywhereEdgeTest {

    private static final CarDescription CAR_1 = new CarDescription("1", 0, 0, FuelType.ELECTRIC, Gearbox.AUTOMATIC, new Provider(2, "PANEK"));
    private static final CarDescription CAR_2 = new CarDescription("2", 0, 0, FuelType.FOSSIL, Gearbox.MANUAL, new Provider(2, "PANEK"));

    private RentVehicleAnywhereEdge edge;
    private RoutingRequest request;
    private State state, rentingState;

    @Before
    public void setUp() {
        Graph graph = new Graph();
        IntersectionVertex v = new IntersectionVertex(graph, "v_name", 0, 0);
        edge = new RentVehicleAnywhereEdge(v);
        request = new RoutingRequest();
        request.setDummyRoutingContext(graph);
        request.setModes(new TraverseModeSet(TraverseMode.WALK, TraverseMode.CAR));
        request.setStartingMode(TraverseMode.WALK);
        state = new State(v, request);
        StateEditor se = state.edit(edge);
        se.beginVehicleRenting(CAR_1);
        rentingState = se.makeState();
    }

    @Test
    public void shouldNotTraverseWhenRentingNotAllowed() {
        // when
        State traversed = edge.traverse(state);

        // then
        assertNull(traversed);
    }

    @Test
    public void shouldNotTraverseWhenNoVehicles() {
        // given
        request.rentingAllowed = true;

        // when
        State traversed = edge.traverse(state);

        // then
        assertNull(traversed);
    }

    @Test
    public void shouldTraverseMultipleVehicles() {
        // given
        edge.getAvailableVehicles().add(CAR_1);
        edge.getAvailableVehicles().add(CAR_2);
        request.rentingAllowed = true;
        // when
        State traversed = edge.traverse(state);

        // then
        assertNotNull(traversed);
        assertEquals(TraverseMode.CAR, traversed.getNonTransitMode());
        assertEquals(CAR_2, traversed.getCurrentVehicle());
        assertNotNull(traversed.getNextResult());
        assertEquals(TraverseMode.CAR, traversed.getNextResult().getNonTransitMode());
        assertEquals(CAR_1, traversed.getNextResult().getCurrentVehicle());
    }

    @Test
    public void shouldFilterVehiclesBasedOnGivenCriteria() {
        // given
        edge.getAvailableVehicles().add(CAR_1);
        edge.getAvailableVehicles().add(CAR_2);
        request.rentingAllowed = true;
        request.vehicleValidator = mock(VehicleValidator.class);
        when(request.vehicleValidator.isValid(CAR_1)).thenReturn(false);
        when(request.vehicleValidator.isValid(CAR_2)).thenReturn(true);

        // when
        State traversed = edge.traverse(state);

        // then
        assertNotNull(traversed);
        assertEquals(TraverseMode.CAR, traversed.getNonTransitMode());
        assertEquals(CAR_2, traversed.getCurrentVehicle());
        assertNull(traversed.getNextResult());
    }

    @Test
    public void shouldReturnNullWhenNoVehiclesMatchCriteria() {
        // given
        edge.getAvailableVehicles().add(CAR_1);
        edge.getAvailableVehicles().add(CAR_2);
        request.rentingAllowed = true;
        request.vehicleValidator = mock(VehicleValidator.class);
        when(request.vehicleValidator.isValid(any())).thenReturn(false);

        // when
        State traversed = edge.traverse(state);

        // then
        assertNull(traversed);
    }

    @Test
    public void shouldNotAllowToDropoffVehicleOutsideParkingZone() {
        // given
        List<ParkingZoneInfo.SingleParkingZone> singleParkingZone = singletonList(new ParkingZoneInfo.SingleParkingZone(2, VehicleType.CAR));
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
        List<ParkingZoneInfo.SingleParkingZone> singleParkingZone = singletonList(new ParkingZoneInfo.SingleParkingZone(2, VehicleType.CAR));
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
        List<ParkingZoneInfo.SingleParkingZone> singleParkingZone = singletonList(new ParkingZoneInfo.SingleParkingZone(2, VehicleType.MOTORBIKE));
        edge.updateParkingZones(singleParkingZone, emptyList());

        // when
        State traversed = edge.traverse(rentingState);

        // then
        assertNotNull(traversed);
    }
}
