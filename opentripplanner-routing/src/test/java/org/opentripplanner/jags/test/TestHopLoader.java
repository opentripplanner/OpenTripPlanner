package org.opentripplanner.jags.test;

import java.io.File;
import java.util.GregorianCalendar;

import junit.framework.TestCase;

import org.opentripplanner.gtfs.GtfsContext;
import org.opentripplanner.gtfs.GtfsLibrary;
import org.opentripplanner.jags.algorithm.Dijkstra;
import org.opentripplanner.jags.core.Edge;
import org.opentripplanner.jags.core.Graph;
import org.opentripplanner.jags.core.State;
import org.opentripplanner.jags.core.Vertex;
import org.opentripplanner.jags.core.TraverseOptions;
import org.opentripplanner.jags.edgetype.Alight;
import org.opentripplanner.jags.edgetype.Board;
import org.opentripplanner.jags.edgetype.Hop;
import org.opentripplanner.jags.edgetype.loader.GTFSHopLoader;
import org.opentripplanner.jags.spt.GraphPath;
import org.opentripplanner.jags.spt.ShortestPathTree;
import org.opentripplanner.jags.vertextypes.TransitStop;

public class TestHopLoader extends TestCase {

    private Graph graph;

    private GtfsContext context;

    public void setUp() throws Exception {

        context = GtfsLibrary.readGtfs(new File(ConstantsForTests.FAKE_GTFS));
        graph = new Graph();

        GTFSHopLoader hl = new GTFSHopLoader(graph, context);
        hl.load();

    }

    public void testBoardAlight() throws Exception {

        Vertex stop_a = graph.getVertex("agency_A");
        Vertex stop_b = graph.getVertex("agency_B");

        assertEquals(2, stop_a.getDegreeOut());
        assertEquals(6, stop_b.getDegreeOut());

        for (Edge e : stop_a.outgoing) {
            assertEquals(Board.class, e.payload.getClass());
        }

        Vertex journey_a_1 = stop_a.outgoing.get(0).tov;

        assertEquals(1, journey_a_1.getDegreeIn());

        for (Edge e : journey_a_1.outgoing) {
            if (e.tov.type == TransitStop.class) {
                assertEquals(Alight.class, e.payload.getClass());
            } else {
                assertEquals(Hop.class, e.payload.getClass());
            }
        }
    }

    public void testRouting() throws Exception {

        Vertex stop_a = graph.getVertex("agency_A");
        Vertex stop_b = graph.getVertex("agency_B");
        Vertex stop_c = graph.getVertex("agency_C");
        Vertex stop_d = graph.getVertex("agency_D");
        Vertex stop_e = graph.getVertex("agency_E");

        TraverseOptions options = new TraverseOptions();
        options.setGtfsContext(context);

        ShortestPathTree spt;
        GraphPath path;

        // A to B
        spt = Dijkstra.getShortestPathTree(graph, stop_a.label, stop_b.label, new State(
                new GregorianCalendar(2009, 8, 7, 0, 0, 0).getTimeInMillis()), options);

        path = spt.getPath(stop_b);
        assertNotNull(path);
        assertEquals(4, path.vertices.size());

        // A to C
        spt = Dijkstra.getShortestPathTree(graph, stop_a.label, stop_c.label, new State(
                new GregorianCalendar(2009, 8, 7, 0, 0, 0).getTimeInMillis()), options);

        path = spt.getPath(stop_c);
        assertNotNull(path);
        assertEquals(5, path.vertices.size());

        // A to D
        spt = Dijkstra.getShortestPathTree(graph, stop_a.label, stop_d.label, new State(
                new GregorianCalendar(2009, 8, 7, 0, 0, 0).getTimeInMillis()), options);

        path = spt.getPath(stop_d);
        assertNotNull(path);
        assertEquals(8, path.vertices.size());

        // A to E
        spt = Dijkstra.getShortestPathTree(graph, stop_a.label, stop_e.label, new State(
                new GregorianCalendar(2009, 8, 7, 0, 0, 0).getTimeInMillis()), options);

        path = spt.getPath(stop_e);
        assertNotNull(path);
        assertEquals(9, path.vertices.size());
    }
}
