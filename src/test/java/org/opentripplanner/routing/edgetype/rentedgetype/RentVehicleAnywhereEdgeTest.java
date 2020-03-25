package org.opentripplanner.routing.edgetype.rentedgetype;

import org.junit.Before;
import org.junit.Test;
import org.opentripplanner.routing.core.RoutingRequest;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.core.TraverseMode;
import org.opentripplanner.routing.core.vehicle_sharing.*;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.vertextype.IntersectionVertex;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.junit.Assert.*;

public class RentVehicleAnywhereEdgeTest {

    private static final CarDescription CAR_1 = new CarDescription(0, 0, FuelType.ELECTRIC, Gearbox.AUTOMAT, Provider.INNOGY);
    private static final CarDescription CAR_2 = new CarDescription(0, 0, FuelType.FOSSIL, Gearbox.MANUAL, Provider.PANEK);

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
        request.vehiclesAllowedToRent = new VehicleDetailsSet(singletonList(FuelType.FOSSIL), emptyList(), emptyList());
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
        request.vehiclesAllowedToRent = new VehicleDetailsSet(singletonList(FuelType.ELECTRIC), singletonList(Gearbox.MANUAL), emptyList());
        // when
        State traversed = edge.traverse(s);

        // then
        assertNull(traversed);
    }
}
