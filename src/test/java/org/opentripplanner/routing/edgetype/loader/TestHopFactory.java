package org.opentripplanner.routing.edgetype.loader;

import com.google.common.collect.Lists;
import junit.framework.TestCase;
import org.junit.Ignore;
import org.opentripplanner.ConstantsForTests;
import org.opentripplanner.gtfs.GtfsContext;
import org.opentripplanner.model.calendar.CalendarServiceData;
import org.opentripplanner.routing.algorithm.astar.AStar;
import org.opentripplanner.routing.api.request.RoutingRequest;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.graph_builder.module.geometry.GeometryAndBlockProcessor;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.routing.spt.GraphPath;
import org.opentripplanner.routing.spt.ShortestPathTree;
import org.opentripplanner.routing.vertextype.TransitStopVertex;
import org.opentripplanner.util.TestUtils;

import java.util.List;

import static org.opentripplanner.gtfs.GtfsContextBuilder.contextBuilder;

/**
 * TODO OTP2 - Test is too close to the implementation and will need to be reimplemented.
 */
@Ignore
public class TestHopFactory extends TestCase {

    private Graph graph;

    private AStar aStar = new AStar();

    private String feedId;

    public void setUp() throws Exception {
        GtfsContext context = contextBuilder(ConstantsForTests.FAKE_GTFS).build();
        graph = new Graph();
        GeometryAndBlockProcessor factory = new GeometryAndBlockProcessor(context);
        factory.run(graph);
        graph.putService(
                CalendarServiceData.class, context.getCalendarServiceData()
        );

        feedId = context.getFeedId().getId();
    }

    public void testDwell() throws Exception {
        Vertex stop_a = graph.getVertex(feedId + ":A_depart");
        Vertex stop_c = graph.getVertex(feedId + ":C_arrive");

        RoutingRequest options = new RoutingRequest();
        options.dateTime = TestUtils.dateInSeconds("America/New_York", 2009, 8, 7, 8, 0, 0);
        options.setRoutingContext(graph, stop_a, stop_c);
        ShortestPathTree spt = aStar.getShortestPathTree(options);

        GraphPath path = spt.getPath(stop_c, false);
        assertNotNull(path);
        assertEquals(6, path.states.size());
        long endTime = TestUtils.dateInSeconds("America/New_York", 2009, 8, 7, 8, 30, 0);
        assertEquals(endTime, path.getEndTime());
    }

    public void testRouting() throws Exception {

        Vertex stop_a = graph.getVertex(feedId + ":A");
        Vertex stop_b = graph.getVertex(feedId + ":B");
        Vertex stop_c = graph.getVertex(feedId + ":C");
        Vertex stop_d = graph.getVertex(feedId + ":D");
        Vertex stop_e = graph.getVertex(feedId + ":E");

        RoutingRequest options = new RoutingRequest();
        options.dateTime = TestUtils.dateInSeconds("America/New_York", 2009, 8, 7, 0, 0, 0); 

        ShortestPathTree spt;
        GraphPath path;

        // A to B
        options.setRoutingContext(graph, stop_a, stop_b);
        spt = aStar.getShortestPathTree(options);

        path = spt.getPath(stop_b, false);
        assertNotNull(path);
        assertEquals(extractStopVertices(path), Lists.newArrayList(stop_a, stop_b));


        // A to C
        options.setRoutingContext(graph, stop_a, stop_c);
        spt = aStar.getShortestPathTree(options);

        path = spt.getPath(stop_c, false);
        assertNotNull(path);
        assertEquals(extractStopVertices(path), Lists.newArrayList(stop_a, stop_c));


        // A to D
        options.setRoutingContext(graph, stop_a, stop_d);
        spt = aStar.getShortestPathTree(options);

        path = spt.getPath(stop_d, false);
        assertNotNull(path);
        assertEquals(extractStopVertices(path), Lists.newArrayList(stop_a, stop_c, stop_d));
        long endTime = TestUtils.dateInSeconds("America/New_York", 2009, 8, 7, 0, 0, 0) + 40 * 60;
        assertEquals(endTime, path.getEndTime());

        // A to E
        options.setRoutingContext(graph, stop_a, stop_e);
        spt = aStar.getShortestPathTree(options);

        path = spt.getPath(stop_e, false);
        assertNotNull(path);
        assertEquals(extractStopVertices(path), Lists.newArrayList(stop_a, stop_c, stop_e));
        endTime = TestUtils.dateInSeconds("America/New_York", 2009, 8, 7, 0, 0, 0) + 70 * 60;
        assertEquals(endTime, path.getEndTime());
    }

    private List<Vertex> extractStopVertices(GraphPath path) {
        List<Vertex> ret = Lists.newArrayList();
        for (State state : path.states) {
            if (state.getVertex() instanceof TransitStopVertex) {
                ret.add(state.getVertex());
            }
        }
        return ret;
    }
}
