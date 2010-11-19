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

import static org.opentripplanner.common.IterableLibrary.*;
import java.io.File;
import java.util.GregorianCalendar;

import junit.framework.TestCase;

import org.opentripplanner.ConstantsForTests;
import org.opentripplanner.gtfs.GtfsContext;
import org.opentripplanner.gtfs.GtfsLibrary;
import org.opentripplanner.routing.algorithm.AStar;
import org.opentripplanner.routing.core.DirectEdge;
import org.opentripplanner.routing.core.Edge;
import org.opentripplanner.routing.core.Graph;
import org.opentripplanner.routing.core.GraphVertex;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.core.TraverseOptions;
import org.opentripplanner.routing.core.Vertex;
import org.opentripplanner.routing.edgetype.PatternAlight;
import org.opentripplanner.routing.edgetype.PatternBoard;
import org.opentripplanner.routing.edgetype.PatternHop;
import org.opentripplanner.routing.edgetype.factory.GTFSPatternHopFactory;
import org.opentripplanner.routing.spt.GraphPath;
import org.opentripplanner.routing.spt.ShortestPathTree;
import org.opentripplanner.routing.core.TransitStop;

public class TestHopFactory extends TestCase {

    private Graph graph;

    private GtfsContext context;

    public void setUp() throws Exception {

        context = GtfsLibrary.readGtfs(new File(ConstantsForTests.FAKE_GTFS));
        graph = new Graph();

        GTFSPatternHopFactory factory = new GTFSPatternHopFactory(context);
        factory.run(graph);

    }

    public void testBoardAlight() throws Exception {

        GraphVertex stop_a = graph.getGraphVertex("agency_A_depart");
        GraphVertex stop_b_depart = graph.getGraphVertex("agency_B_depart");

        assertEquals(1, stop_a.getDegreeOut());
        assertEquals(3, stop_b_depart.getDegreeOut());

        for (Edge e : stop_a.getOutgoing()) {
            assertEquals(PatternBoard.class, e.getClass());
        }

        PatternBoard pb = (PatternBoard) stop_a.getOutgoing().iterator().next();
        GraphVertex journey_a_1 = graph.getGraphVertex(pb.getToVertex());

        assertEquals(1, journey_a_1.getDegreeIn());

        for (DirectEdge e : filter(journey_a_1.getOutgoing(),DirectEdge.class)) {
            if (e.getToVertex() instanceof TransitStop) {
                assertEquals(PatternAlight.class, e.getClass());
            } else {
                assertEquals(PatternHop.class, e.getClass());
            }
        }
    }

    public void testDwell() throws Exception {
        Vertex stop_a = graph.getVertex("agency_A_depart");
        Vertex stop_c = graph.getVertex("agency_C_arrive");

        TraverseOptions options = new TraverseOptions(context);

        ShortestPathTree spt = AStar.getShortestPathTree(graph, stop_a.getLabel(), stop_c
                .getLabel(),
                new State(new GregorianCalendar(2009, 8, 7, 8, 0, 0).getTimeInMillis()), options);

        GraphPath path = spt.getPath(stop_c);
        assertNotNull(path);
        assertEquals(6, path.vertices.size());
        long endTime = new GregorianCalendar(2009, 8, 7, 8, 30, 0).getTimeInMillis();
        assertEquals(endTime, path.vertices.lastElement().state.getTime());

    }

    public void testRouting() throws Exception {

        Vertex stop_a = graph.getVertex("agency_A_depart");
        Vertex stop_b = graph.getVertex("agency_B_arrive");
        Vertex stop_c = graph.getVertex("agency_C_arrive");
        Vertex stop_d = graph.getVertex("agency_D_arrive");
        Vertex stop_e = graph.getVertex("agency_E_arrive");

        TraverseOptions options = new TraverseOptions();
        options.setGtfsContext(context);

        ShortestPathTree spt;
        GraphPath path;

        // A to B
        spt = AStar.getShortestPathTree(graph, stop_a.getLabel(), stop_b.getLabel(), new State(
                new GregorianCalendar(2009, 8, 7, 0, 0, 0).getTimeInMillis()), options);

        path = spt.getPath(stop_b);
        assertNotNull(path);
        assertEquals(4, path.vertices.size());

        // A to C
        spt = AStar.getShortestPathTree(graph, stop_a.getLabel(), stop_c.getLabel(), new State(
                new GregorianCalendar(2009, 8, 7, 0, 0, 0).getTimeInMillis()), options);

        path = spt.getPath(stop_c);
        assertNotNull(path);
        assertEquals(6, path.vertices.size());

        // A to D
        spt = AStar.getShortestPathTree(graph, stop_a.getLabel(), stop_d.getLabel(), new State(
                new GregorianCalendar(2009, 8, 7, 0, 0, 0).getTimeInMillis()), options);

        path = spt.getPath(stop_d);
        assertNotNull(path);
        assertTrue(path.vertices.size() <= 11);
        long endTime = new GregorianCalendar(2009, 8, 7, 0, 0, 0).getTimeInMillis() + 40 * 60 * 1000;
        assertEquals(endTime, path.vertices.lastElement().state.getTime());

        // A to E
        spt = AStar.getShortestPathTree(graph, stop_a.getLabel(), stop_e.getLabel(), new State(
                new GregorianCalendar(2009, 8, 7, 0, 0, 0).getTimeInMillis()), options);

        path = spt.getPath(stop_e);
        assertNotNull(path);
        assertTrue(path.vertices.size() <= 12);
        endTime = new GregorianCalendar(2009, 8, 7, 0, 0, 0).getTimeInMillis() + 70 * 60 * 1000;
        assertEquals(endTime, path.vertices.lastElement().state.getTime());
    }
}
