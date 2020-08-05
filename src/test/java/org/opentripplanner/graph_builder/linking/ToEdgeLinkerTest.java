package org.opentripplanner.graph_builder.linking;

import org.junit.Before;
import org.junit.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.linearref.LinearLocation;
import org.locationtech.jts.linearref.LocationIndexedLine;
import org.opentripplanner.common.geometry.GeometryUtils;
import org.opentripplanner.graph_builder.services.DefaultStreetEdgeFactory;
import org.opentripplanner.routing.edgetype.StreetEdge;
import org.opentripplanner.routing.edgetype.StreetTraversalPermission;
import org.opentripplanner.routing.location.StreetLocation;
import org.opentripplanner.routing.location.TemporaryStreetLocation;
import org.opentripplanner.routing.vertextype.StreetVertex;

import static org.mockito.Mockito.*;

public class ToEdgeLinkerTest {

    private StreetSplitter streetSplitter;
    private EdgesMaker edgesMaker;
    private LinkingGeoTools linkingGeoTools;

    private ToEdgeLinker toEdgeLinker;

    private TemporaryStreetLocation temporaryStreetLocation;
    private StreetVertex vertex;
    private StreetEdge edge;
    private LinearLocation ll;

    @Before
    public void setUp() {
        streetSplitter = mock(StreetSplitter.class);
        edgesMaker = mock(EdgesMaker.class);
        linkingGeoTools = mock(LinkingGeoTools.class);
        toEdgeLinker = new ToEdgeLinker(new DefaultStreetEdgeFactory(), streetSplitter, edgesMaker, linkingGeoTools, false);

        Coordinate coordinate = new Coordinate(1, 2);
        temporaryStreetLocation = new TemporaryStreetLocation("id1", coordinate, null, false);
        vertex = new StreetLocation("id1", coordinate, "name");

        StreetVertex from = new StreetLocation("id1", new Coordinate(0, 1), "name");
        StreetVertex to = new StreetLocation("id2", new Coordinate(1, 1), "name");
        edge = new StreetEdge(from, to, GeometryUtils.makeLineString(0, 1, 0.5, 1, 1, 1),
                "S. Crystal Dr", 100, StreetTraversalPermission.PEDESTRIAN, false);

        ll = new LocationIndexedLine(edge.getGeometry()).getStartIndex();
    }

    @Test
    public void shouldLinkVertexOnEdgeTemporarily() {
        // when
        toEdgeLinker.linkVertexToEdgeTemporarily(temporaryStreetLocation, edge, ll);

        // then
        verify(streetSplitter, times(1)).splitTemporarily(edge, ll, temporaryStreetLocation.isEndVertex());
        verify(edgesMaker, times(1)).makeTemporaryEdges(eq(temporaryStreetLocation), any());
        verifyNoMoreInteractions(streetSplitter, edgesMaker);
        verifyZeroInteractions(linkingGeoTools);
    }

    @Test
    public void shouldLinkVertexOnEdgePermanently() {
        // when
        toEdgeLinker.linkVertexToEdgePermanently(vertex, edge, ll);

        // then
        verify(streetSplitter, times(1)).splitPermanently(edge, ll);
        verify(edgesMaker, times(1)).makePermanentEdges(eq(vertex), any());
        verifyNoMoreInteractions(streetSplitter, edgesMaker);
        verifyZeroInteractions(linkingGeoTools);
    }
}
