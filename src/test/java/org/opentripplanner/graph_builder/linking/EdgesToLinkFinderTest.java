package org.opentripplanner.graph_builder.linking;

import org.junit.Before;
import org.junit.Test;
import org.locationtech.jts.geom.Coordinate;
import org.opentripplanner.common.geometry.GeometryUtils;
import org.opentripplanner.common.geometry.HashGridSpatialIndex;
import org.opentripplanner.routing.core.TraverseMode;
import org.opentripplanner.routing.core.vehicle_sharing.FuelType;
import org.opentripplanner.routing.core.vehicle_sharing.Gearbox;
import org.opentripplanner.routing.core.vehicle_sharing.MotorbikeDescription;
import org.opentripplanner.routing.core.vehicle_sharing.Provider;
import org.opentripplanner.routing.edgetype.StreetEdge;
import org.opentripplanner.routing.edgetype.StreetTraversalPermission;
import org.opentripplanner.routing.graph.Edge;
import org.opentripplanner.routing.location.StreetLocation;
import org.opentripplanner.routing.vertextype.StreetVertex;

import java.util.List;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

public class EdgesToLinkFinderTest {

    private static final MotorbikeDescription MOTORBIKE = new MotorbikeDescription("id", 1, 2, FuelType.ELECTRIC, Gearbox.AUTOMATIC, new Provider(1, "HopCity"));

    private HashGridSpatialIndex<Edge> index;

    private BestCandidatesGetter bestCandidatesGetter;

    private EdgesToLinkFinder edgesToLinkFinder;

    private StreetVertex vertex;
    private StreetEdge edgeWalk, edgeCar, highway;

    @Before
    public void setUp() {
        index = mock(HashGridSpatialIndex.class);
        LinkingGeoTools linkingGeoTools = mock(LinkingGeoTools.class);
        bestCandidatesGetter = mock(BestCandidatesGetter.class);
        edgesToLinkFinder = new EdgesToLinkFinder(index, linkingGeoTools, bestCandidatesGetter);

        vertex = new StreetLocation("id1", new Coordinate(0, 1), "name");

        StreetVertex from = new StreetLocation("id2", new Coordinate(0, 1), "name");
        StreetVertex to = new StreetLocation("id3", new Coordinate(1, 1), "name");
        edgeWalk = new StreetEdge(from, to, GeometryUtils.makeLineString(0, 1, 0.5, 1, 1, 1),
                "S. Crystal Dr", 100, StreetTraversalPermission.PEDESTRIAN, false);
        edgeCar = new StreetEdge(from, to, GeometryUtils.makeLineString(0, 1, 0.5, 1, 1, 1),
                "S. Crystal Dr", 100, StreetTraversalPermission.CAR, false);
        highway = new StreetEdge(from, to, GeometryUtils.makeLineString(0, 1, 0.5, 1, 1, 1),
                "S. Crystal Dr", 100, StreetTraversalPermission.CAR, false);
        highway.setMaxStreetTraverseSpeed(33.3f); // 120km/h
    }

    @Test
    public void shouldReturnClosestEdge() {
        // given
        when(index.query(any())).thenReturn(singletonList(edgeWalk));
        List<StreetEdge> bestEdges = singletonList(edgeWalk);
        when(bestCandidatesGetter.getBestCandidates(eq(singletonList(edgeWalk)), any())).thenReturn(bestEdges);

        // when
        List<StreetEdge> returnedEdges = edgesToLinkFinder.findEdgesToLink(vertex, TraverseMode.WALK);

        // then
        assertSame(returnedEdges, bestEdges);
    }

    @Test
    public void shouldFilterEdgesBasedOnTraverseMode() {
        // given
        when(index.query(any())).thenReturn(singletonList(edgeCar));
        when(bestCandidatesGetter.getBestCandidates(eq(emptyList()), any())).thenReturn(emptyList());

        // when
        List<StreetEdge> returnedEdges = edgesToLinkFinder.findEdgesToLink(vertex, TraverseMode.WALK);

        // then
        assertTrue(returnedEdges.isEmpty());
    }

    @Test
    public void shouldAllowWalkingBike() {
        // given
        when(index.query(any())).thenReturn(singletonList(edgeWalk));
        List<StreetEdge> bestEdges = singletonList(edgeWalk);
        when(bestCandidatesGetter.getBestCandidates(eq(singletonList(edgeWalk)), any())).thenReturn(bestEdges);

        // when
        List<StreetEdge> returnedEdges = edgesToLinkFinder.findEdgesToLink(vertex, TraverseMode.BICYCLE);

        // then
        assertSame(returnedEdges, bestEdges);
    }

    @Test
    public void shouldAllowLinkingMotorbikeToNormalRoad() {
        // given
        when(index.query(any())).thenReturn(singletonList(edgeCar));
        List<StreetEdge> bestEdges = singletonList(edgeCar);
        when(bestCandidatesGetter.getBestCandidates(eq(singletonList(edgeCar)), any())).thenReturn(bestEdges);

        // when
        List<StreetEdge> returnedEdges = edgesToLinkFinder.findEdgesToLinkVehicle(vertex, MOTORBIKE);

        // then
        assertSame(returnedEdges, bestEdges);
    }

    @Test
    public void shouldForbidLinkingMotorbikeToHighway() {
        // given
        when(index.query(any())).thenReturn(singletonList(highway));
        when(bestCandidatesGetter.getBestCandidates(eq(emptyList()), any())).thenReturn(emptyList());

        // when
        List<StreetEdge> returnedEdges = edgesToLinkFinder.findEdgesToLinkVehicle(vertex, MOTORBIKE);

        // then
        assertTrue(returnedEdges.isEmpty());
        verify(bestCandidatesGetter, times(1)).getBestCandidates(eq(emptyList()), any());
    }
}
