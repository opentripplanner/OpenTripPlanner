package org.opentripplanner.graph_builder.linking;

import org.junit.Before;
import org.junit.Test;
import org.opentripplanner.common.model.GenericLocation;
import org.opentripplanner.routing.core.RoutingRequest;
import org.opentripplanner.routing.core.TraverseMode;
import org.opentripplanner.routing.core.TraverseModeSet;
import org.opentripplanner.routing.core.vehicle_sharing.*;
import org.opentripplanner.routing.edgetype.rentedgetype.EdgeWithParkingZones;
import org.opentripplanner.routing.edgetype.rentedgetype.RentVehicleEdge;
import org.opentripplanner.routing.edgetype.rentedgetype.SingleParkingZone;
import org.opentripplanner.routing.edgetype.rentedgetype.TemporaryDropoffVehicleEdge;
import org.opentripplanner.routing.graph.Edge;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.location.TemporaryStreetLocation;
import org.opentripplanner.routing.vertextype.TemporaryRentVehicleVertex;
import org.opentripplanner.updater.vehicle_sharing.parking_zones.ParkingZonesCalculator;

import java.util.List;
import java.util.Optional;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

public class TemporaryStreetSplitterTest {

    private static final CarDescription CAR = new CarDescription("1", 0, 0, FuelType.ELECTRIC, Gearbox.AUTOMATIC, new Provider(2, "PANEK"));

    private final List<SingleParkingZone> parkingZonesEnabled = singletonList(new SingleParkingZone(1, VehicleType.CAR));
    private final List<SingleParkingZone> parkingZonesForEdge = singletonList(new SingleParkingZone(1, VehicleType.CAR));

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
    public void shouldSetTraverseModeToStartingModeWhenRoutingWithRentingVehicles() {
        // given
        routingRequest.startingMode = TraverseMode.WALK;
        when(toStreetEdgeLinker.linkTemporarily(any(), any(), eq(routingRequest))).thenReturn(true);

        // when
        TemporaryStreetLocation closestVertex = temporaryStreetSplitter.linkLocationToGraph(genericLocation, routingRequest, false);

        // then
        verify(toStreetEdgeLinker, times(1)).linkTemporarily(closestVertex, TraverseMode.WALK, routingRequest);
    }

    @Test
    public void shouldSetTraverseModeToCarWhenRoutingTaxi() {
        // given
        routingRequest.startingMode = TraverseMode.CAR;
        when(toStreetEdgeLinker.linkTemporarily(any(), any(), eq(routingRequest))).thenReturn(true);

        // when
        TemporaryStreetLocation closestVertex = temporaryStreetSplitter.linkLocationToGraph(genericLocation, routingRequest, false);

        // then
        verify(toStreetEdgeLinker, times(1)).linkTemporarily(closestVertex, TraverseMode.CAR, routingRequest);
    }

    @Test
    public void shouldSetTraverseModeToWalkWhenEndOfParkAndRide() {
        // given
        routingRequest.parkAndRide = true;
        when(toStreetEdgeLinker.linkTemporarily(any(), any(), eq(routingRequest))).thenReturn(true);

        // when
        TemporaryStreetLocation closestVertex = temporaryStreetSplitter.linkLocationToGraph(genericLocation, routingRequest, true);

        // then
        verify(toStreetEdgeLinker, times(1)).linkTemporarily(closestVertex, TraverseMode.WALK, routingRequest);
    }

    @Test
    public void shouldSetTraverseModeToBicycleWhenRoutingBicycle() {
        // given
        routingRequest.modes = new TraverseModeSet(TraverseMode.BICYCLE);
        when(toStreetEdgeLinker.linkTemporarily(any(), any(), eq(routingRequest))).thenReturn(true);

        // when
        TemporaryStreetLocation closestVertex = temporaryStreetSplitter.linkLocationToGraph(genericLocation, routingRequest, false);

        // then
        verify(toStreetEdgeLinker, times(1)).linkTemporarily(closestVertex, TraverseMode.BICYCLE, routingRequest);
    }

    @Test
    public void shouldSetTraverseModeToCarWhenRoutingCar() {
        // given
        routingRequest.modes = new TraverseModeSet(TraverseMode.CAR, TraverseMode.WALK);
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

        List<SingleParkingZone> parkingZonesEnabled = emptyList();
        List<SingleParkingZone> parkingZonesForEdge = emptyList();
        graph.parkingZonesCalculator = mock(ParkingZonesCalculator.class);
        when(graph.parkingZonesCalculator.getNewParkingZonesEnabled()).thenReturn(parkingZonesEnabled);
        when(graph.parkingZonesCalculator.getParkingZonesForEdge(any(), eq(parkingZonesEnabled))).thenReturn(parkingZonesForEdge);

        // when
        TemporaryStreetLocation closestVertex = temporaryStreetSplitter.linkLocationToGraph(genericLocation, routingRequest, true);

        // then
        assertEquals(1, closestVertex.getOutgoing().size());
        Edge edge = closestVertex.getOutgoing().stream().findFirst().get();
        assertTrue(closestVertex.getIncoming().contains(edge));
        assertTrue(edge instanceof TemporaryDropoffVehicleEdge);
        verify(graph.parkingZonesCalculator, times(1)).getNewParkingZonesEnabled();
        verify(graph.parkingZonesCalculator, times(1)).getParkingZonesForEdge((EdgeWithParkingZones) edge, parkingZonesEnabled);
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

    @Test
    public void shouldReturnEmptyIfFailedToLinkVertex() {
        // given
        when(toStreetEdgeLinker.linkTemporarilyBothWays(any(), any())).thenReturn(false);

        // when
        Optional<TemporaryRentVehicleVertex> temporaryRentVehicleVertex = temporaryStreetSplitter.linkRentableVehicleToGraph(CAR);

        // then
        assertFalse(temporaryRentVehicleVertex.isPresent());
        verify(toStreetEdgeLinker, times(1)).linkTemporarilyBothWays(any(), eq(CAR));
    }

    @Test
    public void shouldReturnVertexIfSucceededInLinking() {
        // given
        when(toStreetEdgeLinker.linkTemporarilyBothWays(any(), any())).thenReturn(true);

        // when
        Optional<TemporaryRentVehicleVertex> temporaryRentVehicleVertex = temporaryStreetSplitter.linkRentableVehicleToGraph(CAR);

        // then
        assertTrue(temporaryRentVehicleVertex.isPresent());
        TemporaryRentVehicleVertex vertex = temporaryRentVehicleVertex.get();
        assertEquals(1, vertex.getIncoming().size());
        assertEquals(1, vertex.getOutgoing().size());
        assertEquals(vertex.getIncoming(), vertex.getOutgoing());
        assertEquals(CAR.getLatitude(), vertex.getLat(), 0.1);
        assertEquals(CAR.getLongitude(), vertex.getLon(), 0.1);
        Edge edge = vertex.getOutgoing().stream().findFirst().get();
        assertTrue(edge instanceof RentVehicleEdge);
        RentVehicleEdge rentVehicleEdge = (RentVehicleEdge) edge;
        assertEquals(CAR, rentVehicleEdge.getVehicle());
        verify(toStreetEdgeLinker, times(1)).linkTemporarilyBothWays(vertex, CAR);
    }

    @Test
    public void shouldAddParkingZonesForVehicleVertex() {
        // given
        graph.parkingZonesCalculator = mock(ParkingZonesCalculator.class);
        when(graph.parkingZonesCalculator.getNewParkingZonesEnabled()).thenReturn(parkingZonesEnabled);
        when(graph.parkingZonesCalculator.getParkingZonesForEdge(any(), any())).thenReturn(parkingZonesForEdge);
        when(toStreetEdgeLinker.linkTemporarilyBothWays(any(), any())).thenReturn(true);

        // when
        Optional<TemporaryRentVehicleVertex> temporaryRentVehicleVertex = temporaryStreetSplitter.linkRentableVehicleToGraph(CAR);

        // then
        assertTrue(temporaryRentVehicleVertex.isPresent());
        TemporaryRentVehicleVertex vertex = temporaryRentVehicleVertex.get();
        assertEquals(1, vertex.getIncoming().size());
        assertEquals(1, vertex.getOutgoing().size());
        assertEquals(vertex.getIncoming(), vertex.getOutgoing());
        Edge edge = vertex.getOutgoing().stream().findFirst().get();

        verify(graph.parkingZonesCalculator, times(1)).getNewParkingZonesEnabled();
        verify(graph.parkingZonesCalculator, times(1)).getParkingZonesForEdge((EdgeWithParkingZones) edge, parkingZonesEnabled);
        verify(toStreetEdgeLinker, times(1)).linkTemporarilyBothWays(vertex, CAR);
    }
}
