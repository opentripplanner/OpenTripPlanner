/* This program is free software: you can redistribute it and/or
   modify it under the terms of the GNU Lesser General Public License
   as published by the Free Software Foundation, either version 3 of
   the License, or (at your option) any later version.
   
   This program is distributed in the hope that it will be useful,
   but WITHOUT ANY WARRANTY; without even the implied warranty of
   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
   GNU General Public License for more details.
   
   You should have received a copy of the GNU General Public License
   along with this program.  If not, see <http://www.gnu.org/licenses/>. 
*/

/* This program is free software: you can redistribute it and/or
modify it under the terms of the GNU Lesser General Public License
as published by the Free Software Foundation, either version 3 of
the License, or (at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program.  If not, see <http://www.gnu.org/licenses/>. */
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
import org.opentripplanner.routing.core.TraverseModeSet;
import org.opentripplanner.routing.core.TraverseOptions;
import org.opentripplanner.routing.core.Vertex;
import org.opentripplanner.routing.edgetype.Alight;
import org.opentripplanner.routing.edgetype.PatternAlight;
import org.opentripplanner.routing.edgetype.PatternBoard;
import org.opentripplanner.routing.edgetype.PatternDwell;
import org.opentripplanner.routing.edgetype.PatternHop;
import org.opentripplanner.routing.edgetype.Transfer;
import org.opentripplanner.routing.edgetype.loader.GTFSPatternHopLoader;
import org.opentripplanner.routing.spt.GraphPath;
import org.opentripplanner.routing.spt.SPTEdge;
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
        long startTime = new GregorianCalendar(2009, 8, 19, 0, 5, 0).getTimeInMillis();

        spt = Dijkstra.getShortestPathTree(graph, stop_g.getLabel(), stop_h.getLabel(), new State(
                startTime), options);

        path = spt.getPath(stop_h);
        assertNotNull(path);
        assertEquals(4, path.vertices.size());
        long endTime = path.vertices.lastElement().state.getTime();
        assertTrue(endTime < startTime + 1000 * 60 * 60);
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

    public void testShapeByDistance() throws Exception {
        Vertex stop_i = graph.getVertex("agency_I");
        Edge hop = getHopOut(stop_i);
        Geometry geometry = hop.getGeometry();
        assertTrue(geometry.getLength() > 1.0);
        assertTrue(geometry.getLength() < 2.0);
    }

    public void testTransfers() throws Exception {
        Vertex stop_k = graph.getVertex("agency_K");
        Vertex stop_n = graph.getVertex("agency_N");
        int transfers = 0;
        for (Edge e : stop_n.getOutgoing()) {
            if (e instanceof Transfer) {
                assertEquals(e.getToVertex(), stop_k);
                transfers += 1;
            }
        }
        assertTrue(transfers > 0);
    }

    public void testInterlining() throws Exception {
        Vertex stop_i = graph.getVertex("agency_I");
        Vertex stop_k = graph.getVertex("agency_K");

        long startTime = new GregorianCalendar(2009, 8, 19, 12, 0, 0).getTimeInMillis();
        TraverseOptions options = new TraverseOptions(context);

        ShortestPathTree spt = Dijkstra.getShortestPathTree(graph, stop_i.getLabel(), stop_k
                .getLabel(), new State(startTime), options);
        GraphPath path = spt.getPath(stop_k);
        int num_alights = 0;
        for (SPTEdge e : path.edges) {
            if (e.payload instanceof PatternAlight || e.payload instanceof Alight) {
                num_alights += 1;
            }
            if (e.payload instanceof PatternDwell) {
                State state0 = e.fromv.state;
                State state1 = e.tov.state;
                assertEquals(10 * 60 * 1000, state1.getTime() - state0.getTime());
            }
        }
        assertEquals(1, num_alights);
    }
    
    public void testTraverseMode() throws Exception {
        Vertex stop_a = graph.getVertex("agency_A");
        Vertex stop_b = graph.getVertex("agency_B");

        ShortestPathTree spt;

        TraverseOptions options = new TraverseOptions(context);
        options.modes = new TraverseModeSet("TRAINISH");

        spt = Dijkstra.getShortestPathTree(graph, stop_a.getLabel(), stop_b.getLabel(), new State(
                new GregorianCalendar(2009, 8, 0, 0, 0, 0).getTimeInMillis()), options );

        //a to b is bus only
        assertNull(spt.getPath(stop_b));
        
        options.modes = new TraverseModeSet("TRAINISH,BUSISH");
        spt = Dijkstra.getShortestPathTree(graph, stop_a.getLabel(), stop_b.getLabel(), new State(
                new GregorianCalendar(2009, 8, 0, 0, 0, 0).getTimeInMillis()), options );

        assertNotNull(spt.getPath(stop_b));
    }
}
