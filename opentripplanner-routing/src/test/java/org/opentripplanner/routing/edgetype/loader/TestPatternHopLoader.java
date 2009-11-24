package org.opentripplanner.routing.edgetype.loader;

import java.io.File;
import java.util.GregorianCalendar;

import junit.framework.TestCase;

import org.opentripplanner.ConstantsForTests;
import org.opentripplanner.gtfs.GtfsContext;
import org.opentripplanner.gtfs.GtfsLibrary;
import org.opentripplanner.routing.algorithm.Dijkstra;
import org.opentripplanner.routing.core.Edge;
import org.opentripplanner.routing.core.Graph;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.core.TraverseOptions;
import org.opentripplanner.routing.core.Vertex;
import org.opentripplanner.routing.edgetype.Alight;
import org.opentripplanner.routing.edgetype.PatternBoard;
import org.opentripplanner.routing.edgetype.PatternHop;
import org.opentripplanner.routing.edgetype.Transfer;
import org.opentripplanner.routing.edgetype.loader.GTFSPatternHopLoader;
import org.opentripplanner.routing.spt.GraphPath;
import org.opentripplanner.routing.spt.ShortestPathTree;
import org.opentripplanner.routing.vertextypes.TransitStop;

import com.vividsolutions.jts.geom.Geometry;

public class TestPatternHopLoader extends TestCase {

    private Graph graph;

    private GtfsContext context;

    public void setUp() throws Exception {

        context = GtfsLibrary.readGtfs(new File(ConstantsForTests.FAKE_GTFS));
        graph = new Graph();

        GTFSPatternHopLoader hl = new GTFSPatternHopLoader(graph, context);
        hl.load();

    }

    public void testBoardAlight() throws Exception {

        Vertex stop_a = graph.getVertex("agency_A");
        Vertex stop_b = graph.getVertex("agency_B");

        assertEquals(1, stop_a.getDegreeOut());
        assertEquals(3, stop_b.getDegreeOut());

        for (Edge e : stop_a.getOutgoing()) {
            assertEquals(PatternBoard.class, e.getClass());
        }

        Vertex journey_a_1 = stop_a.getOutgoing().iterator().next().getToVertex();

        assertEquals(1, journey_a_1.getDegreeIn());

        for (Edge e : journey_a_1.getOutgoing()) {
            if (e.getToVertex().getType() == TransitStop.class) {
                assertEquals(Alight.class, e.getClass());
            } else {
                assertEquals(PatternHop.class, e.getClass());
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
        spt = Dijkstra.getShortestPathTree(graph, stop_a.getLabel(), stop_b.getLabel(), new State(
                new GregorianCalendar(2009, 8, 7, 0, 0, 0).getTimeInMillis()), options);

        path = spt.getPath(stop_b);
        assertNotNull(path);
        assertEquals(4, path.vertices.size());

        // A to C
        spt = Dijkstra.getShortestPathTree(graph, stop_a.getLabel(), stop_c.getLabel(), new State(
                new GregorianCalendar(2009, 8, 7, 0, 0, 0).getTimeInMillis()), options);

        path = spt.getPath(stop_c);
        assertNotNull(path);
        assertEquals(6, path.vertices.size());

        // A to D
        spt = Dijkstra.getShortestPathTree(graph, stop_a.getLabel(), stop_d.getLabel(), new State(
                new GregorianCalendar(2009, 8, 7, 0, 0, 0).getTimeInMillis()), options);

        path = spt.getPath(stop_d);
        assertNotNull(path);
        assertEquals(9, path.vertices.size());

        spt = Dijkstra.getShortestPathTree(graph, stop_a.getLabel(), stop_e.getLabel(), new State(
                new GregorianCalendar(2009, 8, 7, 0, 0, 0).getTimeInMillis()), options);

        path = spt.getPath(stop_e);
        assertNotNull(path);
        assertEquals(11, path.vertices.size());
    }

    public void testRoutingOverMidnight() throws Exception {
        // this route only runs on weekdays
        Vertex stop_g = graph.getVertex("agency_G");
        Vertex stop_h = graph.getVertex("agency_H");

        TraverseOptions options = new TraverseOptions();
        options.setGtfsContext(context);

        ShortestPathTree spt;
        GraphPath path;

        // Friday evening
        spt = Dijkstra.getShortestPathTree(graph, stop_g.getLabel(), stop_h.getLabel(), new State(
                new GregorianCalendar(2009, 8, 18, 23, 20, 0).getTimeInMillis()), options);

        path = spt.getPath(stop_h);
        assertNotNull(path);
        assertEquals(4, path.vertices.size());

        // Saturday morning
        spt = Dijkstra.getShortestPathTree(graph, stop_g.getLabel(), stop_h.getLabel(), new State(
                new GregorianCalendar(2009, 8, 19, 0, 5, 0).getTimeInMillis()), options);

        path = spt.getPath(stop_h);
        assertNotNull(path);
        assertEquals(4, path.vertices.size());

    }

    public Edge getHopOut(Vertex v) {
        for (Edge e : v.getOutgoing()) {
            if (e instanceof PatternBoard) {
                for (Edge f : e.getToVertex().getOutgoing()) {
                    if (f instanceof PatternHop) {
                        return f;
                    }
                }
            }
        }
        return null;
    }
    
    public void testShapeByLocation() throws Exception {
        Vertex stop_g = graph.getVertex("agency_G");
        Edge hop = getHopOut(stop_g);
        Geometry geometry = hop.getGeometry();
        assertTrue(geometry.getLength() > 1.0);
        
        Vertex stop_a = graph.getVertex("agency_A");
        hop = getHopOut(stop_a);
        geometry = hop.getGeometry();
        assertTrue(geometry.getLength() > 0.999);
        assertTrue(geometry.getLength() < 1.001);

    }
    public void testShapeBydistance() throws Exception {
        Vertex stop_i = graph.getVertex("agency_I");
        Edge hop = getHopOut(stop_i);
        Geometry geometry = hop.getGeometry();
        assertTrue(geometry.getLength() > 1.0);
        assertTrue(geometry.getLength() < 2.0);
    }

    public void testTransfers() throws Exception {
        Vertex stop_k = graph.getVertex("agency_K");
        Vertex stop_l = graph.getVertex("agency_L");
        Vertex stop_m = graph.getVertex("agency_M");
        Edge hop = null;
        int transfers = 0;
        for (Edge e : stop_k.getOutgoing()) {
            if (e instanceof Transfer) {
                assertEquals(e.getToVertex(), stop_l);
                transfers += 1;
            }
        }
        assertTrue(transfers > 0);
    }

}
