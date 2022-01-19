package org.opentripplanner.routing.edgetype;

import static org.junit.jupiter.api.Assertions.assertEquals;

import ch.poole.openinghoursparser.OpeningHoursParseException;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import org.junit.jupiter.api.Test;
import org.opentripplanner.routing.algorithm.GraphRoutingTest;
import org.opentripplanner.routing.algorithm.astar.AStar;
import org.opentripplanner.routing.algorithm.raptor.router.street.AccessEgressRouter;
import org.opentripplanner.routing.algorithm.raptor.transit.TransitTuningParameters;
import org.opentripplanner.routing.algorithm.raptor.transit.mappers.AccessEgressMapper;
import org.opentripplanner.routing.algorithm.raptor.transit.mappers.TransitLayerMapper;
import org.opentripplanner.routing.api.request.RoutingRequest;
import org.opentripplanner.routing.api.request.StreetMode;
import org.opentripplanner.routing.core.OsmOpeningHours;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.core.StateEditor;
import org.opentripplanner.routing.core.TimeRestriction;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.routing.vertextype.StreetVertex;
import org.opentripplanner.routing.vertextype.TransitStopVertex;

/**
 * This tests the four ways time restrictions may be used on edges: 1. normal departAt searches 2.
 * normal arriveBy searches 3. access departAt searches 4. egress arriveBy searches
 */
public class TimeRestrictedEdgeTest extends GraphRoutingTest {

    private static final int TRAVERSAL_TIME = 180;

    // A thursday
    private static final ZonedDateTime START_OF_TIME =
            ZonedDateTime.of(2021, 5, 20, 12, 0, 0, 0, ZoneId.of("GMT"));

    private Graph graph;
    private StreetVertex A, B, C;
    private TransitStopVertex S;

    public void createGraph(TimeRestriction a_b, TimeRestriction b_c) {
        graph = graphOf(new Builder() {
            @Override
            public void build() {
                A = intersection("A", 47.500, 19.000);
                B = intersection("B", 47.510, 19.000);
                C = intersection("C", 47.520, 19.000);

                S = stop("S", 47.520, 19.001);

                biLink(C, S);

                new TimeRestrictedTestEdge(A, B, 60, a_b, true, true);
                new TimeRestrictedTestEdge(B, C, 120, b_c, false, false);

                new TimeRestrictedTestEdge(B, A, 60, a_b, false, true);
                new TimeRestrictedTestEdge(C, B, 120, b_c, true, false);
            }
        });

        graph.index();

        graph.setTransitLayer(TransitLayerMapper.map(TransitTuningParameters.FOR_TEST, graph));
    }

    @Test
    public void testNoRestrictions() {
        createGraph(null, null);
        assertTraversal(TRAVERSAL_TIME, -TRAVERSAL_TIME);
        assertAccessAndEgress(0, 0);
    }

    @Test
    public void testAlwaysOpen() throws OpeningHoursParseException {
        createGraph(OsmOpeningHours.parseFromOsm("24/7"), OsmOpeningHours.parseFromOsm("24/7"));
        assertTraversal(TRAVERSAL_TIME, -TRAVERSAL_TIME);
        assertAccessAndEgress(0, 0);
    }

    @Test
    public void testOnlyFirstAndClosed() throws OpeningHoursParseException {
        createGraph(OsmOpeningHours.parseFromOsm("Mo-Su 10:00-11:00,14:00-16:00"), null);
        assertTraversal(
                // There is a two-hour wait at the beginning, along with traversing all the edges
                2 * 60 * 60 + TRAVERSAL_TIME,
                // Since the wait is at the end of the search, there is no extra time for traversal
                -60 * 60
        );
        assertAccessAndEgress(2 * 60 * 60, -(60 * 60) + TRAVERSAL_TIME);
    }

    @Test
    public void testOnlySecondAndFullyClosed() throws OpeningHoursParseException {
        createGraph(null, OsmOpeningHours.parseFromOsm("Mo-Su 10:00-11:00,14:00-16:00"));
        // It is not possible to traverse, since waiting at the end is not allowed
        assertTraversal(
                null,
                null
        );
        assertAccessAndEgress(2 * 60 * 60 - TRAVERSAL_TIME, -60 * 60);
    }

    @Test
    public void testOnlySecondAndPartiallyClosed() throws OpeningHoursParseException {
        createGraph(null, OsmOpeningHours.parseFromOsm("Mo-Su 10:00-12:00,14:00-16:00"));
        assertTraversal(
                // It is not possible to traverse, since waiting at the end is not allowed
                null,
                -TRAVERSAL_TIME
        );
        assertAccessAndEgress(2 * 60 * 60 - TRAVERSAL_TIME, 0);
    }

    @Test
    public void testBothFirstClosedSecondOpen() throws OpeningHoursParseException {
        createGraph(
                OsmOpeningHours.parseFromOsm("Mo-Su 10:00-11:00,14:00-16:00"),
                OsmOpeningHours.parseFromOsm("Mo-Su 10:00-16:00")
        );
        assertTraversal(2 * 60 * 60 + TRAVERSAL_TIME, -60 * 60);
        assertAccessAndEgress(2 * 60 * 60, -60 * 60 + 180);
    }

    @Test
    public void testBothFirstOpenSecondClosed() throws OpeningHoursParseException {
        createGraph(
                OsmOpeningHours.parseFromOsm("Mo-Su 10:00-16:00"),
                OsmOpeningHours.parseFromOsm("Mo-Su 10:00-11:00,14:00-16:00")
        );
        assertTraversal(null, null);
        assertAccessAndEgress(2 * 60 * 60 - 180, -60 * 60);
    }

    @Test
    public void testBothClosedWithWait() throws OpeningHoursParseException {
        createGraph(
                OsmOpeningHours.parseFromOsm("Mo-Su 10:00-11:00,13:00-14:00"),
                OsmOpeningHours.parseFromOsm("Mo-Su 13:00-14:00")
        );
        assertTraversal(60 * 60 + TRAVERSAL_TIME, null);
        assertAccessAndEgress(60 * 60, -22 * 60 * 60);
    }

    @Test
    public void testBothClosed() throws OpeningHoursParseException {
        createGraph(
                OsmOpeningHours.parseFromOsm("Mo-Su 9:00-11:00"),
                OsmOpeningHours.parseFromOsm("Mo-Su 10:00-11:00")
        );
        assertTraversal(null, null);
        assertAccessAndEgress(
                // The first time both restrictions may be traversed is the next day at 09:57
                21 * 60 * 60 + 57 * 60,
                -60 * 60
        );
    }

    private void assertTraversal(Integer departAtDuration, Integer arriveByDuration) {
        assertEquals(departAtDuration, traversalDuration(false), "departAt duration");
        assertEquals(arriveByDuration, traversalDuration(true), "arriveBy duration");
    }

    private void assertAccessAndEgress(int earliestDepartureTime, int latestArrivalTime) {
        assertAccess(earliestDepartureTime, latestArrivalTime);
        assertEgress(earliestDepartureTime, latestArrivalTime);
    }

    private Integer traversalDuration(boolean arriveBy) {
        var options = new RoutingRequest().getStreetSearchRequest(StreetMode.WALK);
        options.setDateTime(START_OF_TIME.toInstant());
        options.arriveBy = arriveBy;
        options.setRoutingContext(graph, A, C);

        var tree = new AStar().getShortestPathTree(options);
        var path = tree.getPath(
                arriveBy ? A : C,
                false
        );

        if (path == null) {
            return null;
        }

        return (int) Duration.between(
                START_OF_TIME,
                Instant.ofEpochSecond(options.arriveBy ? path.getStartTime() : path.getEndTime())
                        .atZone(START_OF_TIME.getZone())
        ).getSeconds();
    }

    private void assertAccess(int earliestDepartureTime, int latestArrivalTime) {
        var rr = new RoutingRequest().getStreetSearchRequest(StreetMode.WALK);
        rr.setRoutingContext(graph, A, null);

        var stops = AccessEgressRouter.streetSearch(rr, StreetMode.WALK, false);
        assertEquals(1, stops.size(), "nearby access stops");

        var accessEgress =
                new AccessEgressMapper(graph.getTransitLayer().getStopIndex())
                        .mapNearbyStop(stops.iterator().next(), START_OF_TIME, false);

        assertEquals(
                earliestDepartureTime,
                accessEgress.earliestDepartureTime(0),
                "access earliestDepartureTime"
        );
        assertEquals(
                latestArrivalTime,
                accessEgress.latestArrivalTime(0),
                "access latestArrivalTime"
        );
    }

    private void assertEgress(int earliestDepartureTime, int latestArrivalTime) {
        var rr = new RoutingRequest().getStreetSearchRequest(StreetMode.WALK);
        rr.setRoutingContext(graph, null, A);

        var stops = AccessEgressRouter.streetSearch(rr, StreetMode.WALK, true);
        assertEquals(1, stops.size(), "nearby egress stops");

        var accessEgress =
                new AccessEgressMapper(graph.getTransitLayer().getStopIndex())
                        .mapNearbyStop(stops.iterator().next(), START_OF_TIME, true);

        assertEquals(
                earliestDepartureTime,
                accessEgress.earliestDepartureTime(0),
                "egress earliestDepartureTime"
        );
        assertEquals(
                latestArrivalTime,
                accessEgress.latestArrivalTime(0),
                "egress latestArrivalTime"
        );
    }

    private static class TimeRestrictedTestEdge extends FreeEdge implements TimeRestrictedEdge {

        private final int traversalTime;
        private final TimeRestriction timeRestriction;
        private final boolean restrictionAtFrom;
        private final boolean allowWaiting;

        public TimeRestrictedTestEdge(
                Vertex from,
                Vertex to,
                int traversalTime,
                TimeRestriction timeRestriction,
                boolean restrictionAtFrom,
                boolean allowWaiting
        ) {
            super(from, to);
            this.traversalTime = traversalTime;
            this.timeRestriction = timeRestriction;
            this.restrictionAtFrom = restrictionAtFrom;
            this.allowWaiting = allowWaiting;
        }

        @Override
        public State traverse(State s0) {
            if (isTraversalBlockedByTimeRestriction(s0, allowWaiting, timeRestriction)) {
                return null;
            }

            StateEditor s1 = s0.edit(this);
            s1.incrementWeight(1);

            var checkThenTraverse = s0.getOptions().arriveBy != restrictionAtFrom;
            if (checkThenTraverse) {
                // The edge must open when entering, or the restriction has a time span
                updateEditorWithTimeRestriction(s0, s1, timeRestriction, fromv);
                s1.incrementTimeInSeconds(traversalTime);
            }
            else {
                // The edge must open when exiting
                s1.incrementTimeInSeconds(traversalTime);
                updateEditorWithTimeRestriction(s0, s1, timeRestriction, tov);
            }

            return s1.makeState();
        }
    }
}
