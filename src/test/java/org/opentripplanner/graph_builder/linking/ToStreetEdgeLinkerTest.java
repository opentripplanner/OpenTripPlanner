package org.opentripplanner.graph_builder.linking;

import org.junit.Before;
import org.junit.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.linearref.LinearLocation;
import org.opentripplanner.common.geometry.GeometryUtils;
import org.opentripplanner.routing.core.RoutingRequest;
import org.opentripplanner.routing.core.TraverseMode;
import org.opentripplanner.routing.core.vehicle_sharing.CarDescription;
import org.opentripplanner.routing.core.vehicle_sharing.FuelType;
import org.opentripplanner.routing.core.vehicle_sharing.Gearbox;
import org.opentripplanner.routing.core.vehicle_sharing.Provider;
import org.opentripplanner.routing.edgetype.StreetEdge;
import org.opentripplanner.routing.edgetype.StreetTraversalPermission;
import org.opentripplanner.routing.error.TrivialPathException;
import org.opentripplanner.routing.location.StreetLocation;
import org.opentripplanner.routing.location.TemporaryStreetLocation;
import org.opentripplanner.routing.vertextype.StreetVertex;
import org.opentripplanner.routing.vertextype.TemporaryRentVehicleVertex;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

public class ToStreetEdgeLinkerTest {

    private static final CarDescription CAR = new CarDescription("1", 0, 0, FuelType.ELECTRIC, Gearbox.AUTOMATIC, new Provider(2, "PANEK"));

    private ToEdgeLinker toEdgeLinker;
    private EdgesToLinkFinder edgesToLinkFinder;
    private LinkingGeoTools linkingGeoTools;
    private EdgesMaker edgesMaker;

    private ToStreetEdgeLinker toStreetEdgeLinker;

    private TemporaryStreetLocation temporaryVertex, otherTemporaryVertex;
    private TemporaryRentVehicleVertex temporaryRentVehicleVertex;
    private StreetVertex vertex;
    private StreetEdge edge, carOnlyEdge;
    private LinearLocation ll;
    private RoutingRequest options;

    @Before
    public void setUp() {
        toEdgeLinker = mock(ToEdgeLinker.class);
        edgesToLinkFinder = mock(EdgesToLinkFinder.class);
        linkingGeoTools = mock(LinkingGeoTools.class);
        edgesMaker = mock(EdgesMaker.class);
        ll = mock(LinearLocation.class);
        when(linkingGeoTools.findLocationClosestToVertex(any(), any())).thenReturn(ll);

        toStreetEdgeLinker = new ToStreetEdgeLinker(toEdgeLinker, edgesToLinkFinder, linkingGeoTools, edgesMaker);

        temporaryVertex = new TemporaryStreetLocation("id1", new Coordinate(0, 0), null, false);
        otherTemporaryVertex = new TemporaryStreetLocation("id1", new Coordinate(0, 0), null, false);
        temporaryRentVehicleVertex = new TemporaryRentVehicleVertex("id3", new Coordinate(0, 0), "name");
        vertex = new StreetLocation("id2", new Coordinate(0, 1), "name");

        StreetVertex from = new StreetLocation("id1", new Coordinate(0, 1), "name");
        StreetVertex to = new StreetLocation("id2", new Coordinate(1, 1), "name");
        edge = new StreetEdge(from, to, GeometryUtils.makeLineString(0, 1, 0.5, 1, 1, 1),
                "S. Crystal Dr", 100, StreetTraversalPermission.PEDESTRIAN, false);
        carOnlyEdge = new StreetEdge(from, to, GeometryUtils.makeLineString(0, 1, 1, 1),
                "S. Crystal Dr", 100, StreetTraversalPermission.CAR, false);

        options = new RoutingRequest();
    }

    @Test
    public void shouldReturnFalseIfNoTemporaryLinksWereMade() {
        // given
        when(edgesToLinkFinder.findEdgesToLink(any(), any())).thenReturn(emptyList());

        // when
        boolean linkTemporarily = toStreetEdgeLinker.linkTemporarily(temporaryVertex, TraverseMode.WALK, options);

        // then
        assertFalse(linkTemporarily);
        verify(edgesToLinkFinder, times(1)).findEdgesToLink(temporaryVertex, TraverseMode.WALK);
        verifyNoMoreInteractions(edgesToLinkFinder);
        verifyZeroInteractions(toEdgeLinker, linkingGeoTools, edgesMaker);
    }

    @Test
    public void shouldReturnFalseIfNoPermanentLinksWereMade() {
        // given
        when(edgesToLinkFinder.findEdgesToLink(any(), any())).thenReturn(emptyList());

        // when
        boolean linkPermanently = toStreetEdgeLinker.linkPermanently(vertex, TraverseMode.WALK);

        // then
        assertFalse(linkPermanently);
        verify(edgesToLinkFinder, times(1)).findEdgesToLink(vertex, TraverseMode.WALK);
        verifyNoMoreInteractions(edgesToLinkFinder);
        verifyZeroInteractions(toEdgeLinker, linkingGeoTools, edgesMaker);
    }

    @Test
    public void shouldReturnTrueIfTemporaryLinkWasMadeToBeginningOfEdge() {
        // given
        when(edgesToLinkFinder.findEdgesToLink(any(), any())).thenReturn(singletonList(edge));
        when(linkingGeoTools.isLocationAtTheBeginning(any())).thenReturn(true);

        // when
        boolean linkTemporarily = toStreetEdgeLinker.linkTemporarily(temporaryVertex, TraverseMode.WALK, options);

        // then
        assertTrue(linkTemporarily);
        verify(edgesMaker, times(1)).makeTemporaryEdges(temporaryVertex, edge.getFromVertex()); // link was made to the beginning of edge

        verify(edgesToLinkFinder, times(1)).findEdgesToLink(temporaryVertex, TraverseMode.WALK);
        verify(linkingGeoTools, times(1)).findLocationClosestToVertex(temporaryVertex, edge.getGeometry());
        verify(linkingGeoTools, times(1)).isLocationAtTheBeginning(ll);
        verifyNoMoreInteractions(edgesToLinkFinder, linkingGeoTools, edgesMaker);
        verifyZeroInteractions(toEdgeLinker);
    }

    @Test
    public void shouldReturnTrueIfTemporaryLinkWasMadeToEndOfEdge() {
        // given
        when(edgesToLinkFinder.findEdgesToLink(any(), any())).thenReturn(singletonList(edge));
        when(linkingGeoTools.isLocationAtTheBeginning(any())).thenReturn(false);
        when(linkingGeoTools.isLocationExactlyAtTheEnd(any(), any())).thenReturn(true);

        // when
        boolean linkTemporarily = toStreetEdgeLinker.linkTemporarily(temporaryVertex, TraverseMode.WALK, options);

        // then
        assertTrue(linkTemporarily);
        verify(edgesMaker, times(1)).makeTemporaryEdges(temporaryVertex, edge.getToVertex()); // link was made to end of edge

        verify(edgesToLinkFinder, times(1)).findEdgesToLink(temporaryVertex, TraverseMode.WALK);
        verify(linkingGeoTools, times(1)).findLocationClosestToVertex(temporaryVertex, edge.getGeometry());
        verify(linkingGeoTools, times(1)).isLocationAtTheBeginning(ll);
        verify(linkingGeoTools, times(1)).isLocationExactlyAtTheEnd(ll, edge.getGeometry());
        verifyNoMoreInteractions(edgesToLinkFinder, linkingGeoTools, edgesMaker);
        verifyZeroInteractions(toEdgeLinker);
    }

    @Test
    public void shouldReturnTrueIfTemporaryLinkWasMadeToEndOfEdgeWhenProjectionIsExactlyAtTheEnd() {
        // given
        when(edgesToLinkFinder.findEdgesToLink(any(), any())).thenReturn(singletonList(edge));
        when(linkingGeoTools.isLocationAtTheBeginning(any())).thenReturn(false);
        when(linkingGeoTools.isLocationExactlyAtTheEnd(any(), any())).thenReturn(false);
        when(linkingGeoTools.isLocationAtTheEnd(any(), any())).thenReturn(true);

        // when
        boolean linkTemporarily = toStreetEdgeLinker.linkTemporarily(temporaryVertex, TraverseMode.WALK, options);

        // then
        assertTrue(linkTemporarily);
        verify(edgesMaker, times(1)).makeTemporaryEdges(temporaryVertex, edge.getToVertex()); // link was made to end of edge

        verify(edgesToLinkFinder, times(1)).findEdgesToLink(temporaryVertex, TraverseMode.WALK);
        verify(linkingGeoTools, times(1)).findLocationClosestToVertex(temporaryVertex, edge.getGeometry());
        verify(linkingGeoTools, times(1)).isLocationAtTheBeginning(ll);
        verify(linkingGeoTools, times(1)).isLocationExactlyAtTheEnd(ll, edge.getGeometry());
        verify(linkingGeoTools, times(1)).isLocationAtTheEnd(ll, edge.getGeometry());
        verifyNoMoreInteractions(edgesToLinkFinder, linkingGeoTools, edgesMaker);
        verifyZeroInteractions(toEdgeLinker);
    }

    @Test
    public void shouldReturnTrueIfPermanentLinkWasMade() {
        // given
        when(edgesToLinkFinder.findEdgesToLink(any(), any())).thenReturn(singletonList(edge));
        when(linkingGeoTools.isLocationAtTheBeginning(any())).thenReturn(true);

        // when
        boolean linkPermanently = toStreetEdgeLinker.linkPermanently(vertex, TraverseMode.WALK);

        // then
        assertTrue(linkPermanently);
        verify(edgesMaker, times(1)).makePermanentEdges(vertex, (StreetVertex) edge.getFromVertex());

        verify(edgesToLinkFinder, times(1)).findEdgesToLink(vertex, TraverseMode.WALK);
        verify(linkingGeoTools, times(1)).findLocationClosestToVertex(vertex, edge.getGeometry());
        verify(linkingGeoTools, times(1)).isLocationAtTheBeginning(ll);
        verifyNoMoreInteractions(edgesToLinkFinder, linkingGeoTools, edgesMaker);
        verifyZeroInteractions(toEdgeLinker);
    }

    @Test
    public void shouldSplitEdgeTemporarilyWhenCannotLinkToBeginningOrEnd() {
        // given
        when(edgesToLinkFinder.findEdgesToLink(any(), any())).thenReturn(singletonList(edge));
        when(linkingGeoTools.isLocationAtTheBeginning(any())).thenReturn(false);
        when(linkingGeoTools.isLocationExactlyAtTheEnd(any(), any())).thenReturn(false);
        when(linkingGeoTools.isLocationAtTheEnd(any(), any())).thenReturn(false);

        // when
        boolean linkTemporarily = toStreetEdgeLinker.linkTemporarily(temporaryVertex, TraverseMode.WALK, options);

        // then
        assertTrue(linkTemporarily);
        verify(toEdgeLinker, times(1)).linkVertexToEdgeTemporarily(temporaryVertex, edge, ll); // link to edge

        verify(edgesToLinkFinder, times(1)).findEdgesToLink(temporaryVertex, TraverseMode.WALK);
        verify(linkingGeoTools, times(1)).findLocationClosestToVertex(temporaryVertex, edge.getGeometry());
        verify(linkingGeoTools, times(1)).isLocationAtTheBeginning(ll);
        verify(linkingGeoTools, times(1)).isLocationExactlyAtTheEnd(ll, edge.getGeometry());
        verify(linkingGeoTools, times(1)).isLocationAtTheEnd(ll, edge.getGeometry());
        verifyNoMoreInteractions(edgesToLinkFinder, linkingGeoTools, toEdgeLinker);
        verifyZeroInteractions(edgesMaker);
    }

    @Test
    public void shouldSplitEdgePermanentlyWhenCannotLinkToBeginningOrEnd() {
        // given
        when(edgesToLinkFinder.findEdgesToLink(any(), any())).thenReturn(singletonList(edge));
        when(linkingGeoTools.isLocationAtTheBeginning(any())).thenReturn(false);
        when(linkingGeoTools.isLocationExactlyAtTheEnd(any(), any())).thenReturn(false);
        when(linkingGeoTools.isLocationAtTheEnd(any(), any())).thenReturn(false);

        // when
        boolean linkPermanently = toStreetEdgeLinker.linkPermanently(vertex, TraverseMode.WALK);

        // then
        assertTrue(linkPermanently);
        verify(toEdgeLinker, times(1)).linkVertexToEdgePermanently(vertex, edge, ll); // link to edge

        verify(edgesToLinkFinder, times(1)).findEdgesToLink(vertex, TraverseMode.WALK);
        verify(linkingGeoTools, times(1)).findLocationClosestToVertex(vertex, edge.getGeometry());
        verify(linkingGeoTools, times(1)).isLocationAtTheBeginning(ll);
        verify(linkingGeoTools, times(1)).isLocationExactlyAtTheEnd(ll, edge.getGeometry());
        verify(linkingGeoTools, times(1)).isLocationAtTheEnd(ll, edge.getGeometry());
        verifyNoMoreInteractions(edgesToLinkFinder, linkingGeoTools, toEdgeLinker);
        verifyZeroInteractions(edgesMaker);
    }

    @Test(expected = TrivialPathException.class)
    public void shouldThrowExceptionWhenTryingToMakeSecondTemporarySplitOnTheSameEdge() {
        // given
        when(edgesToLinkFinder.findEdgesToLink(any(), any())).thenReturn(singletonList(edge));
        when(linkingGeoTools.isLocationAtTheBeginning(any())).thenReturn(false);
        when(linkingGeoTools.isLocationExactlyAtTheEnd(any(), any())).thenReturn(false);
        when(linkingGeoTools.isLocationAtTheEnd(any(), any())).thenReturn(false);

        toStreetEdgeLinker.linkTemporarily(temporaryVertex, TraverseMode.WALK, options); // first linking

        // when
        toStreetEdgeLinker.linkTemporarily(otherTemporaryVertex, TraverseMode.WALK, options);
    }

    @Test
    public void shouldReturnTrueIfTemporaryLinkWasMadeBothWaysToVertex() {
        // given
        when(edgesToLinkFinder.findEdgesToLinkVehicle(any(), any())).thenReturn(singletonList(edge));
        when(linkingGeoTools.isLocationAtTheBeginning(any())).thenReturn(true);

        // when
        boolean linkTemporarily = toStreetEdgeLinker.linkTemporarilyBothWays(temporaryRentVehicleVertex, CAR);

        // then
        assertTrue(linkTemporarily);
        verify(edgesMaker, times(1)).makeTemporaryEdgesBothWays(temporaryRentVehicleVertex, edge.getFromVertex());

        verify(edgesToLinkFinder, times(1)).findEdgesToLinkVehicle(temporaryRentVehicleVertex, CAR);
        verify(linkingGeoTools, times(1)).findLocationClosestToVertex(temporaryRentVehicleVertex, edge.getGeometry());
        verify(linkingGeoTools, times(1)).isLocationAtTheBeginning(ll);
        verifyNoMoreInteractions(edgesToLinkFinder, linkingGeoTools, edgesMaker);
        verifyZeroInteractions(toEdgeLinker);
    }

    @Test
    public void shouldReturnTrueIfTemporaryLinkWasMadeBothWaysToEdge() {
        // given
        when(edgesToLinkFinder.findEdgesToLinkVehicle(any(), any())).thenReturn(singletonList(edge));
        when(linkingGeoTools.isLocationAtTheBeginning(any())).thenReturn(false);
        when(linkingGeoTools.isLocationExactlyAtTheEnd(any(), any())).thenReturn(false);
        when(linkingGeoTools.isLocationAtTheEnd(any(), any())).thenReturn(false);

        // when
        boolean linkTemporarily = toStreetEdgeLinker.linkTemporarilyBothWays(temporaryRentVehicleVertex, CAR);

        // then
        assertTrue(linkTemporarily);
        verify(toEdgeLinker, times(1)).linkVertexToEdgeBothWaysTemporarily(temporaryRentVehicleVertex, edge, ll);

        verify(edgesToLinkFinder, times(1)).findEdgesToLinkVehicle(temporaryRentVehicleVertex, CAR);
        verify(linkingGeoTools, times(1)).findLocationClosestToVertex(temporaryRentVehicleVertex, edge.getGeometry());
        verifyNoMoreInteractions(edgesToLinkFinder, toEdgeLinker);
        verifyZeroInteractions(edgesMaker);
    }

    @Test
    public void shouldReturnTrueIfTemporaryLinkWasMadeBothWaysToEdgeForcingExtraWalkEdges() {
        // given
        when(edgesToLinkFinder.findEdgesToLinkVehicle(any(), any())).thenReturn(singletonList(carOnlyEdge));
        when(edgesToLinkFinder.findEdgesToLink(any(), any())).thenReturn(singletonList(edge));
        when(linkingGeoTools.isLocationAtTheBeginning(any())).thenReturn(false);
        when(linkingGeoTools.isLocationExactlyAtTheEnd(any(), any())).thenReturn(false);
        when(linkingGeoTools.isLocationAtTheEnd(any(), any())).thenReturn(false);

        // when
        boolean linkTemporarily = toStreetEdgeLinker.linkTemporarilyBothWays(temporaryRentVehicleVertex, CAR);

        // then
        assertTrue(linkTemporarily);
        verify(toEdgeLinker, times(1)).linkVertexToEdgeBothWaysTemporarily(temporaryRentVehicleVertex, edge, ll);
        verify(toEdgeLinker, times(1)).linkVertexToEdgeBothWaysTemporarily(temporaryRentVehicleVertex, carOnlyEdge, ll);

        verify(edgesToLinkFinder, times(1)).findEdgesToLinkVehicle(temporaryRentVehicleVertex, CAR);
        verify(edgesToLinkFinder, times(1)).findEdgesToLink(temporaryRentVehicleVertex, TraverseMode.WALK);
        verify(linkingGeoTools, times(1)).findLocationClosestToVertex(temporaryRentVehicleVertex, carOnlyEdge.getGeometry());
        verify(linkingGeoTools, times(1)).findLocationClosestToVertex(temporaryRentVehicleVertex, edge.getGeometry());
        verifyNoMoreInteractions(edgesToLinkFinder, toEdgeLinker);
        verifyZeroInteractions(edgesMaker);
    }

    @Test
    public void shouldReturnFalseIfFailedToLinkBothWays() {
        // given
        when(edgesToLinkFinder.findEdgesToLinkVehicle(any(), any())).thenReturn(emptyList());
        when(edgesToLinkFinder.findEdgesToLink(any(), any())).thenReturn(emptyList());

        // when
        boolean linkTemporarily = toStreetEdgeLinker.linkTemporarilyBothWays(temporaryRentVehicleVertex, CAR);

        // then
        assertFalse(linkTemporarily);
        verify(edgesToLinkFinder, times(1)).findEdgesToLinkVehicle(temporaryVertex, CAR);
        verify(edgesToLinkFinder, times(1)).findEdgesToLink(temporaryVertex, TraverseMode.WALK);
        verifyNoMoreInteractions(edgesToLinkFinder);
        verifyZeroInteractions(toEdgeLinker, linkingGeoTools, edgesMaker);
    }
}
