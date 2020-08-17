package org.opentripplanner.graph_builder.linking;

import org.junit.Before;
import org.junit.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.linearref.LinearLocation;
import org.opentripplanner.common.geometry.GeometryUtils;
import org.opentripplanner.common.geometry.HashGridSpatialIndex;
import org.opentripplanner.routing.edgetype.StreetEdge;
import org.opentripplanner.routing.edgetype.StreetTraversalPermission;
import org.opentripplanner.routing.edgetype.TemporaryEdge;
import org.opentripplanner.routing.graph.Edge;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.location.StreetLocation;
import org.opentripplanner.routing.vertextype.SplitterVertex;
import org.opentripplanner.routing.vertextype.TemporaryRentVehicleSplitterVertex;

import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

public class StreetSplitterTest {

    private StreetSplitter streetSplitter;

    StreetLocation from;
    StreetLocation to;
    private StreetEdge streetEdge;
    private LinearLocation ll;

    @Before
    public void setUp() {
        Graph graph = new Graph();
        HashGridSpatialIndex<Edge> index = new HashGridSpatialIndex<>();
        streetSplitter = new StreetSplitter(graph, index);

        from = new StreetLocation("id1", new Coordinate(0, 1), "name");
        to = new StreetLocation("id2", new Coordinate(1, 2), "name");
        streetEdge = new StreetEdge(from, to, GeometryUtils.makeLineString(-77.0492, 38.857, -77.0492, 38.858),
                "S. Crystal Dr", 100, StreetTraversalPermission.PEDESTRIAN, false);

        ll = mock(LinearLocation.class);
        when(ll.getCoordinate(any())).thenReturn(new Coordinate(3, 4));
    }

    @Test
    public void shouldSplitEdgeTemporarilyAtOrigin() {
        // when
        SplitterVertex splitterVertex = streetSplitter.splitTemporarily(streetEdge, ll, false);

        // then
        assertEquals(0, splitterVertex.getIncoming().size());
        assertEquals(1, splitterVertex.getOutgoing().size()); // we add one outgoing edge

        Edge temporaryEdge = splitterVertex.getOutgoing().stream().findFirst().get();
        assertTrue(temporaryEdge instanceof TemporaryEdge);
        assertTrue(to.getIncoming().contains(temporaryEdge)); // edge from origin ends in `to` vertex

        assertEquals(0, from.getIncoming().size());
        assertEquals(1, from.getOutgoing().size());
        assertEquals(2, to.getIncoming().size()); // we add one incoming temporary edge
        assertEquals(0, to.getOutgoing().size());

        verify(ll, times(1)).getCoordinate(streetEdge.getGeometry());
    }

    @Test
    public void shouldSplitEdgeTemporarilyAtDestination() {
        // when
        SplitterVertex splitterVertex = streetSplitter.splitTemporarily(streetEdge, ll, true);

        // then
        assertEquals(1, splitterVertex.getIncoming().size()); // we add one incoming edge
        assertEquals(0, splitterVertex.getOutgoing().size());

        Edge temporaryEdge = splitterVertex.getIncoming().stream().findFirst().get();
        assertTrue(temporaryEdge instanceof TemporaryEdge);
        assertTrue(from.getOutgoing().contains(temporaryEdge)); // edge from `from` vertex ends in destination

        assertEquals(0, from.getIncoming().size());
        assertEquals(2, from.getOutgoing().size()); // we add one outgoing temporary edge
        assertEquals(1, to.getIncoming().size());
        assertEquals(0, to.getOutgoing().size());

        verify(ll, times(1)).getCoordinate(streetEdge.getGeometry());
    }

    @Test
    public void shouldSplitEdgePermanently() {
        // when
        SplitterVertex splitterVertex = streetSplitter.splitPermanently(streetEdge, ll);

        // then
        assertEquals(1, splitterVertex.getIncoming().size()); // we add edge `from` -> `splitterVertex`
        assertEquals(1, from.getOutgoing().size());
        assertEquals(from.getOutgoing(), splitterVertex.getIncoming());
        assertFalse(splitterVertex.getIncoming().stream().findFirst().get() instanceof TemporaryEdge);

        assertEquals(1, splitterVertex.getOutgoing().size()); // we add edge `splitterVertex` -> `to`
        assertEquals(1, to.getIncoming().size());
        assertEquals(splitterVertex.getOutgoing(), to.getIncoming());
        assertFalse(splitterVertex.getOutgoing().stream().findFirst().get() instanceof TemporaryEdge);

        assertEquals(0, from.getIncoming().size());
        assertEquals(0, to.getOutgoing().size());

        verify(ll, times(1)).getCoordinate(streetEdge.getGeometry());
    }

    @Test
    public void shouldSplitEdgeTemporarilyWithRentVehicleVertex() {
        // when
        TemporaryRentVehicleSplitterVertex splitterVertex = streetSplitter.splitTemporarilyWithRentVehicleSplitterVertex(streetEdge, ll);

        // then
        assertEquals(1, splitterVertex.getIncoming().size());
        Edge temporaryEdgeToSplitter = splitterVertex.getIncoming().stream().findFirst().get();
        assertTrue(temporaryEdgeToSplitter instanceof TemporaryEdge);
        assertTrue(from.getOutgoing().contains(temporaryEdgeToSplitter));

        assertEquals(1, splitterVertex.getOutgoing().size());
        Edge temporaryEdgeFromSplitter = splitterVertex.getOutgoing().stream().findFirst().get();
        assertTrue(temporaryEdgeFromSplitter instanceof TemporaryEdge);
        assertTrue(to.getIncoming().contains(temporaryEdgeFromSplitter));

        assertEquals(0, from.getIncoming().size());
        assertEquals(2, from.getOutgoing().size()); // we add one outgoing temporary edge
        assertEquals(2, to.getIncoming().size()); // we add one incoming temporary edge
        assertEquals(0, to.getOutgoing().size());

        verify(ll, times(1)).getCoordinate(streetEdge.getGeometry());
    }
}
