package org.opentripplanner.routing.alertpatch;

import junit.framework.TestCase;
import org.junit.Ignore;
import org.opentripplanner.ConstantsForTests;
import org.opentripplanner.graph_builder.module.geometry.GeometryAndBlockProcessor;
import org.opentripplanner.gtfs.GtfsContext;
import org.opentripplanner.model.FeedScopedId;
import org.opentripplanner.model.calendar.CalendarServiceData;
import org.opentripplanner.routing.algorithm.astar.AStar;
import org.opentripplanner.routing.api.request.RoutingRequest;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.graph.Edge;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.routing.spt.GraphPath;
import org.opentripplanner.routing.spt.ShortestPathTree;
import org.opentripplanner.util.TestUtils;

import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;

import static org.opentripplanner.gtfs.GtfsContextBuilder.contextBuilder;

/**
 * TODO OTP2 - Test is too close to the implementation and will need to be reimplemented.
 */
@Ignore
public class AlertPatchTest extends TestCase {
    private Graph graph;

    private RoutingRequest options;

    private AStar aStar;

    private String feedId;

    public void setUp() throws Exception {
        aStar = new AStar();
        options = new RoutingRequest();
        graph = new Graph();

        GtfsContext context = contextBuilder(ConstantsForTests.FAKE_GTFS)
                .withIssueStoreAndDeduplicator(graph)
                .build();
        GeometryAndBlockProcessor factory = new GeometryAndBlockProcessor(context);
        factory.run(graph);

        graph.putService(
                CalendarServiceData.class, context.getCalendarServiceData()
        );
        graph.index();

        feedId = context.getFeedId().getId();
    }

    public void testStopAlertPatch() {
        AlertPatch snp1 = new AlertPatch();
        snp1.setFeedId(feedId);
        snp1.setTimePeriods(Collections.singletonList(new TimePeriod(
                0, 1000L * 60 * 60 * 24 * 365 * 40))); // until ~1/1/2011
        Alert note1 = Alert.createSimpleAlerts("The first note");
        snp1.setAlert(note1);
        snp1.setId("id1");
        snp1.setStop(new FeedScopedId(feedId, "A"));
        snp1.apply(graph);

        Vertex stop_a = graph.getVertex(feedId + ":A");
        Vertex stop_e = graph.getVertex(feedId + ":E_arrive");

        ShortestPathTree spt;
        GraphPath optimizedPath, unoptimizedPath;

        options.dateTime = TestUtils.dateInSeconds("America/New_York", 2009, 8, 7, 0, 0, 0);
        options.setRoutingContext(graph, stop_a, stop_e);
        spt = aStar.getShortestPathTree(options);

        optimizedPath = spt.getPath(stop_e, true);
        unoptimizedPath = spt.getPath(stop_e, false);
        assertNotNull(optimizedPath);
        HashSet<Alert> expectedAlerts = new HashSet<Alert>();
        expectedAlerts.add(note1);

        Edge optimizedEdge = optimizedPath.states.get(1).getBackEdge();
        HashSet<Alert> optimizedAlerts = new HashSet<Alert>();
        for (AlertPatch alertPatch : graph.getAlertPatches(optimizedEdge)) {
            optimizedAlerts.add(alertPatch.getAlert());
        }
        assertEquals(expectedAlerts, optimizedAlerts);

        Edge unoptimizedEdge = unoptimizedPath.states.get(1).getBackEdge();
        HashSet<Alert> unoptimizedAlerts = new HashSet<Alert>();
        for (AlertPatch alertPatch : graph.getAlertPatches(unoptimizedEdge)) {
            unoptimizedAlerts.add(alertPatch.getAlert());
        }
        assertEquals(expectedAlerts, unoptimizedAlerts);
    }

    public void testTimeRanges() {
        AlertPatch snp1 = new AlertPatch();
        snp1.setFeedId(feedId);
        LinkedList<TimePeriod> timePeriods = new LinkedList<TimePeriod>();
        long breakTime = TestUtils.dateInSeconds("America/New_York", 2009, 8, 7, 0, 0, 0);
        timePeriods.add(new TimePeriod(0, breakTime)); // until the beginning of the day
        long secondPeriodStartTime = TestUtils.dateInSeconds("America/New_York", 2009, 8, 7, 7, 0, 0);
        long secondPeriodEndTime = TestUtils.dateInSeconds("America/New_York", 2009, 8, 8, 0, 0, 0);
        timePeriods.add(new TimePeriod(secondPeriodStartTime, secondPeriodEndTime));
        snp1.setTimePeriods(timePeriods);
        Alert note1 = Alert.createSimpleAlerts("The first note");
        snp1.setAlert(note1);
        snp1.setId("id1");
        snp1.setStop(new FeedScopedId(feedId, "A"));
        snp1.apply(graph);

        Vertex stop_a = graph.getVertex(feedId + ":A");
        Vertex stop_e = graph.getVertex(feedId + ":E_arrive");

        ShortestPathTree spt;
        GraphPath path;

        options.dateTime = TestUtils.dateInSeconds("America/New_York", 2009, 8, 7, 0, 0, 0);
        options.setRoutingContext(graph, stop_a, stop_e);
        spt = aStar.getShortestPathTree(options);

        path = spt.getPath(stop_e, true);
        assertNotNull(path);
        // expect no notes because we are during the break
        State noAlertPatchesState = path.states.get(1);
        Edge noAlertPatchesEdge = noAlertPatchesState.getBackEdge();
        HashSet<Alert> noAlertPatchesAlerts = new HashSet<Alert>();
        for (AlertPatch alertPatch : graph.getAlertPatches(noAlertPatchesEdge)) {
            if (alertPatch.displayDuring(noAlertPatchesState)) {
                noAlertPatchesAlerts.add(alertPatch.getAlert());
            }
        }
        assertEquals(new HashSet<Alert>(), noAlertPatchesAlerts);

        // now a trip during the second period
        options.dateTime = TestUtils.dateInSeconds("America/New_York", 2009, 8, 7, 8, 0, 0);
        options.setRoutingContext(graph, stop_a, stop_e);
        spt = aStar.getShortestPathTree(options);

        path = spt.getPath(stop_e, false); // do not optimize because we want the first trip
        assertNotNull(path);
        HashSet<Alert> expectedNotes = new HashSet<Alert>();
        expectedNotes.add(note1);
        State oneAlertPatchState = path.states.get(1);
        Edge oneAlertPatchEdge = oneAlertPatchState.getBackEdge();
        HashSet<Alert> oneAlertPatchAlerts = new HashSet<Alert>();
        for (AlertPatch alertPatch : graph.getAlertPatches(oneAlertPatchEdge)) {
            if (alertPatch.displayDuring(oneAlertPatchState)) {
                oneAlertPatchAlerts.add(alertPatch.getAlert());
            }
        }
        assertEquals(expectedNotes, oneAlertPatchAlerts);
    }

    public void testRouteNotePatch() {
        AlertPatch rnp1 = new AlertPatch();
        rnp1.setFeedId(feedId);

        rnp1.setTimePeriods(Collections.singletonList(new TimePeriod(
                0, 1000L * 60 * 60 * 24 * 365 * 40))); // until ~1/1/2011
        Alert note1 = Alert.createSimpleAlerts("The route note");
        rnp1.setAlert(note1);
        rnp1.setId("id1");
        // Routes isn't patched in tests through GtfsBundle, which is why we have have a reference to agency id here.
        rnp1.setRoute(new FeedScopedId("agency", "1"));
        rnp1.apply(graph);

        Vertex stop_a = graph.getVertex(feedId + ":A");
        Vertex stop_e = graph.getVertex(feedId + ":E_arrive");

        ShortestPathTree spt;
        GraphPath path;

        options.setRoutingContext(graph, stop_a, stop_e);
        spt = aStar.getShortestPathTree(options);

        path = spt.getPath(stop_e, false);
        assertNotNull(path);
        HashSet<Alert> expectedAlerts = new HashSet<Alert>();
        expectedAlerts.add(note1);
        Edge actualEdge = path.states.get(2).getBackEdge();
        HashSet<Alert> actualAlerts = new HashSet<Alert>();
        for (AlertPatch alertPatch : graph.getAlertPatches(actualEdge)) {
            actualAlerts.add(alertPatch.getAlert());
        }
        assertEquals(expectedAlerts, actualAlerts);
    }
}
