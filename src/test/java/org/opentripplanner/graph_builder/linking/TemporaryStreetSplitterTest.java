package org.opentripplanner.graph_builder.linking;

import org.junit.Before;
import org.junit.Test;
import org.opentripplanner.common.model.GenericLocation;
import org.opentripplanner.routing.core.RoutingRequest;
import org.opentripplanner.routing.core.TraverseMode;
import org.opentripplanner.routing.core.TraverseModeSet;
import org.opentripplanner.routing.edgetype.rentedgetype.ParkingZoneInfo;
import org.opentripplanner.routing.edgetype.rentedgetype.TemporaryDropoffVehicleEdge;
import org.opentripplanner.routing.graph.Edge;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.location.TemporaryStreetLocation;
import org.opentripplanner.updater.vehicle_sharing.parking_zones.ParkingZonesCalculator;

import java.util.List;

import static java.util.Collections.emptyList;
import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

public class TemporaryStreetSplitterTest {

    private Graph graph;
    private ToStreetEdgeLinker toStreetEdgeLinker;
    private ToTransitStopLinker toTransitStopLinker;

    private TemporaryStreetSplitter temporaryStreetSplitter;

    RoutingRequest routingRequest;
    GenericLocation genericLocation;

    @Before
    public void setUp() {
        graph = new Graph();

        toStreetEdgeLinker = mock(ToStreetEdgeLinker.class);
        toTransitStopLinker = mock(ToTransitStopLinker.class);
        temporaryStreetSplitter = new TemporaryStreetSplitter(graph, toStreetEdgeLinker, toTransitStopLinker);

        genericLocation = new GenericLocation(10, 23);
        routingRequest = new RoutingRequest();
    }

    @Test
    public void shouldReturnClosestVertexWhenLinkingToEdgeSucceed() {
        // given
        when(toStreetEdgeLinker.linkTemporarily(any(), any(), eq(routingRequest))).thenReturn(true);

        // when
        TemporaryStreetLocation closestVertex = temporaryStreetSplitter.linkLocationToGraph(genericLocation, routingRequest, false);

        // then
        assertEquals(genericLocation.getCoordinate(), closestVertex.getCoordinate());
        assertFalse(closestVertex.isEndVertex());
        verify(toStreetEdgeLinker, times(1)).linkTemporarily(closestVertex, TraverseMode.WALK, routingRequest);
        verifyNoMoreInteractions(toStreetEdgeLinker);
        verifyZeroInteractions(toTransitStopLinker);
    }

    @Test
    public void shouldReturnClosestVertexWhenLinkingToTransitStopSucceeded() {
        // given
        when(toStreetEdgeLinker.linkTemporarily(any(), any(), eq(routingRequest))).thenReturn(false);
        when(toTransitStopLinker.tryLinkVertexToStop(any())).thenReturn(true);

        // when
        TemporaryStreetLocation closestVertex = temporaryStreetSplitter.linkLocationToGraph(genericLocation, routingRequest, false);

        // then
        assertEquals(genericLocation.getCoordinate(), closestVertex.getCoordinate());
        verify(toStreetEdgeLinker, times(1)).linkTemporarily(closestVertex, TraverseMode.WALK, routingRequest);
        verify(toTransitStopLinker, times(1)).tryLinkVertexToStop(closestVertex);
        verifyNoMoreInteractions(toStreetEdgeLinker, toTransitStopLinker);
    }

    @Test
    public void shouldReturnNotLinkedVertexWhenAllLinkingFailed() {
        // given
        when(toStreetEdgeLinker.linkTemporarily(any(), any(), eq(routingRequest))).thenReturn(false);
        when(toTransitStopLinker.tryLinkVertexToStop(any())).thenReturn(false);

        // when
        TemporaryStreetLocation closestVertex = temporaryStreetSplitter.linkLocationToGraph(genericLocation, routingRequest, false);

        // then
        assertEquals(genericLocation.getCoordinate(), closestVertex.getCoordinate());
        verify(toStreetEdgeLinker, times(1)).linkTemporarily(closestVertex, TraverseMode.WALK, routingRequest);
        verify(toTransitStopLinker, times(1)).tryLinkVertexToStop(closestVertex);
        verifyNoMoreInteractions(toStreetEdgeLinker, toTransitStopLinker);
    }

    @Test
    public void shouldSetTraverseModeToCarWhenRoutingCar() {
        // given
        routingRequest.modes = new TraverseModeSet(TraverseMode.CAR);
        when(toStreetEdgeLinker.linkTemporarily(any(), any(), eq(routingRequest))).thenReturn(true);

        // when
        TemporaryStreetLocation closestVertex = temporaryStreetSplitter.linkLocationToGraph(genericLocation, routingRequest, false);

        // then
        verify(toStreetEdgeLinker, times(1)).linkTemporarily(closestVertex, TraverseMode.CAR, routingRequest);
    }

    @Test
    public void shouldAddDropoffVehicleEdgeAtDestination() {
        // given
        when(toStreetEdgeLinker.linkTemporarily(any(), any(), eq(routingRequest))).thenReturn(true);

        List<ParkingZoneInfo.SingleParkingZone> parkingZonesEnabled = emptyList();
        List<ParkingZoneInfo.SingleParkingZone> parkingZonesForEdge = emptyList();
        graph.parkingZonesCalculator = mock(ParkingZonesCalculator.class);
        when(graph.parkingZonesCalculator.getNewParkingZonesEnabled()).thenReturn(parkingZonesEnabled);
        when(graph.parkingZonesCalculator.getParkingZonesForRentEdge(any(), eq(parkingZonesEnabled))).thenReturn(parkingZonesForEdge);

        // when
        TemporaryStreetLocation closestVertex = temporaryStreetSplitter.linkLocationToGraph(genericLocation, routingRequest, true);

        // then
        assertEquals(1, closestVertex.getOutgoing().size());
        Edge edge = closestVertex.getOutgoing().stream().findFirst().get();
        assertTrue(closestVertex.getIncoming().contains(edge));
        assertTrue(edge instanceof TemporaryDropoffVehicleEdge);
        verify(graph.parkingZonesCalculator, times(1)).getNewParkingZonesEnabled();
        verify(graph.parkingZonesCalculator, times(1)).getParkingZonesForRentEdge((TemporaryDropoffVehicleEdge) edge, parkingZonesEnabled);
    }

    @Test
    public void shouldNotAddDropoffVehicleEdgeAtVertexOtherThanDestination() {
        // given
        when(toStreetEdgeLinker.linkTemporarily(any(), any(), eq(routingRequest))).thenReturn(true);

        // when
        TemporaryStreetLocation closestVertex = temporaryStreetSplitter.linkLocationToGraph(genericLocation, routingRequest, false);

        // then
        assertTrue(closestVertex.getOutgoing().isEmpty());
    }
}
