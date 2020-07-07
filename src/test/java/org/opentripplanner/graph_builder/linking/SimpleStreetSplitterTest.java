package org.opentripplanner.graph_builder.linking;


import org.junit.Before;
import org.junit.Test;
import org.opentripplanner.common.model.GenericLocation;
import org.opentripplanner.routing.core.RoutingRequest;
import org.opentripplanner.routing.core.TraverseMode;
import org.opentripplanner.routing.edgetype.rentedgetype.ParkingZoneInfo;
import org.opentripplanner.routing.edgetype.rentedgetype.TemporaryDropoffVehicleEdge;
import org.opentripplanner.routing.graph.Edge;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.updater.vehicle_sharing.parking_zones.ParkingZonesCalculator;

import java.util.List;

import static java.util.Collections.emptyList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

public class SimpleStreetSplitterTest {


    private Graph graph;
    private SimpleStreetSplitter simpleStreetSplitter;
    private SimpleStreetSplitter spySimpleStreetSplitter;


    @Before
    public void buildSpy() {
        graph = new Graph();
        simpleStreetSplitter = new SimpleStreetSplitter(graph, null, null, false);
        spySimpleStreetSplitter = spy(simpleStreetSplitter);
    }

    /**
     * Tests that traverse mode WALK is used when getting closest end vertex for park and ride.
     */
    @Test
    public void testFindEndVertexForParkAndRide() {
        GenericLocation genericLocation = new GenericLocation(10, 23);

        RoutingRequest routingRequest = new RoutingRequest();
        routingRequest.setMode(TraverseMode.CAR);
        routingRequest.parkAndRide = true;

        spySimpleStreetSplitter.getClosestVertex(genericLocation, routingRequest, true);
        verify(spySimpleStreetSplitter).link(any(Vertex.class), eq(TraverseMode.WALK), eq(routingRequest));
    }

    @Test
    public void shouldAddDropoffVehicleEdgeAtDestination() {
        // given
        GenericLocation genericLocation = new GenericLocation(10, 23);
        RoutingRequest routingRequest = new RoutingRequest();
        List<ParkingZoneInfo.SingleParkingZone> parkingZonesEnabled = emptyList();
        List<ParkingZoneInfo.SingleParkingZone> parkingZonesForEdge = emptyList();
        graph.parkingZonesCalculator = mock(ParkingZonesCalculator.class);
        when(graph.parkingZonesCalculator.getNewParkingZonesEnabled()).thenReturn(parkingZonesEnabled);
        when(graph.parkingZonesCalculator.getParkingZonesForRentEdge(any(), eq(parkingZonesEnabled))).thenReturn(parkingZonesForEdge);

        // when
        Vertex closestVertex = simpleStreetSplitter.getClosestVertex(genericLocation, routingRequest, true);

        // then
        assertEquals(1, closestVertex.getOutgoing().size());
        Edge edge = closestVertex.getOutgoing().stream().findFirst().get();
        assertTrue(edge instanceof TemporaryDropoffVehicleEdge);
        verify(graph.parkingZonesCalculator, times(1)).getNewParkingZonesEnabled();
        verify(graph.parkingZonesCalculator, times(1)).getParkingZonesForRentEdge((TemporaryDropoffVehicleEdge) edge, parkingZonesEnabled);
    }

    @Test
    public void shouldNotAddDropoffVehicleEdgeAtVertexOtherThanDestination() {
        // given
        GenericLocation genericLocation = new GenericLocation(10, 23);
        RoutingRequest routingRequest = new RoutingRequest();
        // when
        Vertex closestVertex = simpleStreetSplitter.getClosestVertex(genericLocation, routingRequest, false);

        // then
        assertEquals(0, closestVertex.getOutgoing().size());
    }
}
