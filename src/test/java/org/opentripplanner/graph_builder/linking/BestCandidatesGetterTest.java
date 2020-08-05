package org.opentripplanner.graph_builder.linking;

import org.junit.Before;
import org.junit.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.LineString;
import org.opentripplanner.common.geometry.GeometryUtils;
import org.opentripplanner.routing.edgetype.StreetEdge;
import org.opentripplanner.routing.edgetype.StreetTraversalPermission;
import org.opentripplanner.routing.location.StreetLocation;
import org.opentripplanner.routing.vertextype.StreetVertex;

import java.util.List;
import java.util.function.Function;

import static com.google.common.collect.ImmutableList.of;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.*;

public class BestCandidatesGetterTest {

    private final BestCandidatesGetter bestCandidatesGetter = new BestCandidatesGetter();

    Function<StreetEdge, Double> distanceObtainer;

    private StreetEdge edge1, edgeCloseToEdge1, anotherCloseEdge, edgeFarAway;

    @Before
    public void setUp() {

        StreetVertex from = new StreetLocation("id2", new Coordinate(0, 1), "name");
        StreetVertex to = new StreetLocation("id3", new Coordinate(1, 1), "name");
        LineString lineString = GeometryUtils.makeLineString(0, 1, 0.5, 1, 1, 1);
        edge1 = new StreetEdge(from, to, lineString, "S. Crystal Dr", 100, StreetTraversalPermission.CAR, false);
        edgeCloseToEdge1 = new StreetEdge(from, to, lineString, "S. Crystal Dr", 100, StreetTraversalPermission.CAR, false);
        anotherCloseEdge = new StreetEdge(from, to, lineString, "S. Crystal Dr", 100, StreetTraversalPermission.CAR, false);
        edgeFarAway = new StreetEdge(from, to, lineString, "S. Crystal Dr", 100, StreetTraversalPermission.CAR, false);

        distanceObtainer = mock(Function.class);
        when(distanceObtainer.apply(edge1)).thenReturn(0.0001);
        when(distanceObtainer.apply(edgeCloseToEdge1)).thenReturn(0.0001000000001);
        when(distanceObtainer.apply(anotherCloseEdge)).thenReturn(0.0002);
        when(distanceObtainer.apply(edgeFarAway)).thenReturn(2000.0);
    }

    @Test
    public void shouldReturnClosestCandidate() {
        // when
        List<StreetEdge> bestCandidates = bestCandidatesGetter.getBestCandidates(singletonList(edge1), distanceObtainer);

        // then
        assertEquals(1, bestCandidates.size());
        assertTrue(bestCandidates.contains(edge1));
        verify(distanceObtainer, times(1)).apply(edge1);
        verifyNoMoreInteractions(distanceObtainer);
    }

    @Test
    public void shouldReturnEmptyListIfNoCandidatesAvailable() {
        // when
        List<StreetEdge> bestCandidates = bestCandidatesGetter.getBestCandidates(emptyList(), distanceObtainer);

        // then
        assertTrue(bestCandidates.isEmpty());
        verifyZeroInteractions(distanceObtainer);
    }

    @Test
    public void shouldReturnEmptyListIfCandidateTooFarAway() {
        // when
        List<StreetEdge> bestCandidates = bestCandidatesGetter.getBestCandidates(singletonList(edgeFarAway), distanceObtainer);

        // then
        assertTrue(bestCandidates.isEmpty());
        verify(distanceObtainer, times(1)).apply(edgeFarAway);
        verifyNoMoreInteractions(distanceObtainer);
    }

    @Test
    public void shouldFilterCandidatesBasedOnDistance() {
        // when
        List<StreetEdge> bestCandidates = bestCandidatesGetter.getBestCandidates(of(edge1, anotherCloseEdge), distanceObtainer);

        // then
        assertEquals(1, bestCandidates.size());
        assertTrue(bestCandidates.contains(edge1));
        verify(distanceObtainer, times(1)).apply(edge1);
        verify(distanceObtainer, times(1)).apply(anotherCloseEdge);
        verifyNoMoreInteractions(distanceObtainer);
    }

    @Test
    public void shouldReturnAllCandidatesReallyCloseToEachOther() {
        // when
        List<StreetEdge> bestCandidates = bestCandidatesGetter.getBestCandidates(of(edge1, edgeCloseToEdge1, anotherCloseEdge), distanceObtainer);

        // then
        assertEquals(2, bestCandidates.size());
        assertTrue(bestCandidates.contains(edge1));
        assertTrue(bestCandidates.contains(edgeCloseToEdge1));
        verify(distanceObtainer, times(1)).apply(edge1);
        verify(distanceObtainer, times(1)).apply(edgeCloseToEdge1);
        verify(distanceObtainer, times(1)).apply(anotherCloseEdge);
        verifyNoMoreInteractions(distanceObtainer);
    }
}
