package org.opentripplanner.routing.edgetype.rentedgetype;

import org.junit.Before;
import org.junit.Test;
import org.opentripplanner.routing.core.RoutingRequest;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.core.TraverseMode;
import org.opentripplanner.routing.core.TraverseModeSet;
import org.opentripplanner.routing.core.vehicle_sharing.*;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.vertextype.IntersectionVertex;

import static java.util.Collections.emptySet;
import static java.util.Collections.singleton;
import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class RentVehicleAnywhereEdgeTest {

    private static final CarDescription CAR_1 = new CarDescription("1", 0, 0, FuelType.ELECTRIC, Gearbox.AUTOMATIC, new Provider(2, "PANEK"));
    private static final CarDescription CAR_2 = new CarDescription("2", 0, 0, FuelType.FOSSIL, Gearbox.MANUAL, new Provider(2, "PANEK"));

    private RentVehicleAnywhereEdge edge;
    private RoutingRequest request;
    private State s;

    @Before
    public void setUp() {
        Graph graph = new Graph();
        IntersectionVertex v = new IntersectionVertex(graph, "v_name", 0, 0);
        edge  = new RentVehicleAnywhereEdge(v);
        request = new RoutingRequest();
        request.setDummyRoutingContext(graph);
        request.setModes(new TraverseModeSet(TraverseMode.WALK, TraverseMode.CAR));
        request.setStartingMode(TraverseMode.WALK);
        s = new State(v, request);
    }

    @Test
    public void shouldNotTraverseWhenRentingNotAllowed() {
        // when
        State traversed = edge.traverse(s);

        // then
        assertNull(traversed);
    }

    @Test
    public void shouldNotTraverseWhenNoVehicles() {
        // given
        request.rentingAllowed = true;

        // when
        State traversed = edge.traverse(s);

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
        State traversed = edge.traverse(s);

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
        State traversed = edge.traverse(s);

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
        State traversed = edge.traverse(s);

        // then
        assertNull(traversed);
    }
}
