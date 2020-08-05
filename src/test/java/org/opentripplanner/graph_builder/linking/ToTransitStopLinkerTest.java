package org.opentripplanner.graph_builder.linking;

import org.junit.Before;
import org.junit.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.index.SpatialIndex;
import org.opentripplanner.common.geometry.HashGridSpatialIndex;
import org.opentripplanner.model.FeedScopedId;
import org.opentripplanner.model.Stop;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.location.TemporaryStreetLocation;
import org.opentripplanner.routing.vertextype.TransitStop;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

public class ToTransitStopLinkerTest {

    private SpatialIndex transitStopIndex;

    private EdgesMaker edgesMaker;

    private BestCandidatesGetter bestCandidatesGetter;

    private ToTransitStopLinker toTransitStopLinker;

    private TemporaryStreetLocation temporaryVertex;
    private TransitStop transitStop;

    @Before
    public void setUp() {
        transitStopIndex = mock(HashGridSpatialIndex.class);
        LinkingGeoTools linkingGeoTools = mock(LinkingGeoTools.class);
        edgesMaker = mock(EdgesMaker.class);
        bestCandidatesGetter = mock(BestCandidatesGetter.class);
        toTransitStopLinker = new ToTransitStopLinker(transitStopIndex, linkingGeoTools, edgesMaker, bestCandidatesGetter);

        temporaryVertex = new TemporaryStreetLocation("id1", new Coordinate(0, 1), null, false);

        Graph graph = new Graph();
        Stop stop = new Stop();
        stop.setName("transitVertex 1");
        stop.setLon(-74.005);
        stop.setLat(40.0099999);
        stop.setId(new FeedScopedId("A", "fleem station"));
        transitStop = new TransitStop(graph, stop);
    }

    @Test
    public void shouldLinkToClosestTransitStop() {
        // given
        when(transitStopIndex.query(any())).thenReturn(singletonList(transitStop));
        when(bestCandidatesGetter.getBestCandidates(eq(singletonList(transitStop)), any())).thenReturn(singletonList(transitStop));

        // when
        boolean linked = toTransitStopLinker.tryLinkVertexToStop(temporaryVertex);

        // then
        assertTrue(linked);
        verify(edgesMaker, times(1)).makeTemporaryEdges(temporaryVertex, transitStop);
        verifyNoMoreInteractions(edgesMaker);
    }

    @Test
    public void shouldNotLinkWhenNoTransitStopsNearby() {
        // given
        when(transitStopIndex.query(any())).thenReturn(singletonList(transitStop));
        when(bestCandidatesGetter.getBestCandidates(eq(singletonList(transitStop)), any())).thenReturn(emptyList());

        // when
        boolean linked = toTransitStopLinker.tryLinkVertexToStop(temporaryVertex);

        // then
        assertFalse(linked);
        verifyZeroInteractions(edgesMaker);
    }
}
