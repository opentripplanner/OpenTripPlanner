package org.opentripplanner.routing.edgetype;

import org.junit.Before;
import org.junit.Test;
import org.opentripplanner.model.*;
import org.opentripplanner.model.calendar.ServiceDate;
import org.opentripplanner.routing.core.RoutingContext;
import org.opentripplanner.routing.core.RoutingRequest;
import org.opentripplanner.routing.core.TraverseMode;
import org.opentripplanner.routing.core.TraverseModeSet;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.trippattern.Deduplicator;
import org.opentripplanner.routing.trippattern.TripTimes;
import org.opentripplanner.routing.vertextype.PatternStopVertex;
import org.opentripplanner.updater.stoptime.TimetableSnapshotSource;

import java.util.Date;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class PatternHopTest {

    private Graph graph;
    private Route route;
    private StopPattern sp;
    private final Stop s0 = new Stop();
    private final Stop s1 = new Stop();
    private StopTime st0;
    private StopTime st1;

    @Before
    public void setUp() {
        graph = new Graph();
        route = new Route();

        st0 = new StopTime();
        st0.setStop(s0);
        st0.setDepartureTime(10);
        st1 = new StopTime();
        st1.setStop(s1);
        st1.setArrivalTime(15);

        sp = new StopPattern(asList(st0, st1));
    }

    @Test
    public void testThatTheSnapshotWillBeUsedForToday() {
        TripPattern tp = new TripPattern(route, sp);

        PatternStopVertex v0 = new PatternStopVertex(graph, "maple_0th", tp, s0);
        PatternStopVertex v1 = new PatternStopVertex(graph, "maple_1st", tp, s1);

        final RoutingRequest rrMock = mock(RoutingRequest.class);
        final TimetableSnapshot tsMock = mock(TimetableSnapshot.class);
        final Timetable ttMock = mock(Timetable.class);
        final TimetableSnapshotSource tssMock = mock(TimetableSnapshotSource.class);

        graph.timetableSnapshotSource = tssMock;

        rrMock.modes = new TraverseModeSet(TraverseMode.TRANSIT);
        when(rrMock.getDateTime()).thenReturn(new Date());
        when(tssMock.getTimetableSnapshot()).thenReturn(tsMock);
        when(tsMock.resolve(any(TripPattern.class), any(ServiceDate.class))).thenReturn(ttMock);
        rrMock.rctx = new RoutingContext(rrMock, graph, v0, v1);

        when(ttMock.getBestRunningTime(anyInt())).thenReturn(5);

        assertEquals(5, new PatternHop(v0, v1, s0, s1, 0).timeLowerBound(rrMock), 4);
    }

    @Test
    public void testThatTheScheduledTimetableWillBeUsed() {
        graph = new Graph();

        TripPattern tp = new TripPattern(route, sp);
        tp.add(new TripTimes(new Trip(), asList(st0, st1), new Deduplicator()));
        tp.scheduledTimetable.finish();

        PatternStopVertex v0 = new PatternStopVertex(graph, "maple_0th", tp, s0);
        PatternStopVertex v1 = new PatternStopVertex(graph, "maple_1st", tp, s1);

        final RoutingRequest rrMock = mock(RoutingRequest.class);
        final TimetableSnapshotSource tssMock = mock(TimetableSnapshotSource.class);

        graph.timetableSnapshotSource = tssMock;

        rrMock.modes = new TraverseModeSet(TraverseMode.TRANSIT);
        when(rrMock.getDateTime()).thenReturn(new Date());
        when(tssMock.getTimetableSnapshot()).thenReturn(null);
        rrMock.rctx = new RoutingContext(rrMock, graph, v0, v1);

        assertEquals(5, new PatternHop(v0, v1, s0, s1, 0).timeLowerBound(rrMock), 4);
    }
}
