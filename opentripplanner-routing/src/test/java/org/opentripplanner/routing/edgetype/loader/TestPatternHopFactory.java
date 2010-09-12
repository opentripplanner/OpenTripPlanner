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
import java.util.Calendar;
import java.util.GregorianCalendar;

import junit.framework.TestCase;

import org.opentripplanner.ConstantsForTests;
import org.opentripplanner.common.geometry.GeometryUtils;
import org.opentripplanner.gtfs.GtfsContext;
import org.opentripplanner.gtfs.GtfsLibrary;
import org.opentripplanner.routing.algorithm.AStar;
import org.opentripplanner.routing.core.Edge;
import org.opentripplanner.routing.core.Graph;
import org.opentripplanner.routing.core.GraphVertex;
import org.opentripplanner.routing.core.OptimizeType;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.core.TraverseModeSet;
import org.opentripplanner.routing.core.TraverseOptions;
import org.opentripplanner.routing.core.Vertex;
import org.opentripplanner.routing.edgetype.Alight;
import org.opentripplanner.routing.edgetype.PatternAlight;
import org.opentripplanner.routing.edgetype.PatternBoard;
import org.opentripplanner.routing.edgetype.PatternDwell;
import org.opentripplanner.routing.edgetype.PatternHop;
import org.opentripplanner.routing.edgetype.StreetTransitLink;
import org.opentripplanner.routing.edgetype.StreetVertex;
import org.opentripplanner.routing.edgetype.Transfer;
import org.opentripplanner.routing.edgetype.TurnEdge;
import org.opentripplanner.routing.edgetype.factory.GTFSPatternHopFactory;
import org.opentripplanner.routing.spt.GraphPath;
import org.opentripplanner.routing.spt.SPTEdge;
import org.opentripplanner.routing.spt.ShortestPathTree;
import org.opentripplanner.routing.core.TransitStop;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;

public class TestPatternHopFactory extends TestCase {

    private Graph graph;

    private GtfsContext context;

    public void setUp() throws Exception {

        context = GtfsLibrary.readGtfs(new File(ConstantsForTests.FAKE_GTFS));
        graph = new Graph();

        GTFSPatternHopFactory factory = new GTFSPatternHopFactory(context);
        factory.run(graph);

        Vertex stop_a = graph.getVertex("agency_A");
        Vertex stop_b = graph.getVertex("agency_B");
        Vertex stop_c = graph.getVertex("agency_C");
        Vertex stop_d = graph.getVertex("agency_D");

        StreetVertex near_a = (StreetVertex) graph.addVertex(new StreetVertex("near_a", GeometryUtils.makeLineString(stop_a.getX() + 0.00001, stop_a.getY() + 0.00001, stop_a.getX() - 0.00001, stop_a.getY() - 0.00001), "near a", 100, false));
        StreetVertex near_b = (StreetVertex) graph.addVertex(new StreetVertex("near_b", GeometryUtils.makeLineString(stop_b.getX() + 0.00001, stop_b.getY() + 0.00001, stop_b.getX() - 0.00001, stop_b.getY() - 0.00001), "near b", 100, false));
        StreetVertex near_c = (StreetVertex) graph.addVertex(new StreetVertex("near_c", GeometryUtils.makeLineString(stop_c.getX() + 0.00001, stop_c.getY() + 0.00001, stop_c.getX() - 0.00001, stop_c.getY() - 0.00001), "near c", 100, false));
        StreetVertex near_d = (StreetVertex) graph.addVertex(new StreetVertex("near_d", GeometryUtils.makeLineString(stop_d.getX() + 0.00001, stop_d.getY() + 0.00001, stop_d.getX() - 0.00001, stop_d.getY() - 0.00001), "near d", 100, false));

        StreetVertex[] nearPoints = {near_a, near_b, near_c, near_d};

        Coordinate between = new Coordinate((stop_a.getX() + stop_b.getX()) / 2,
                (stop_a.getY() + stop_b.getY()) / 2);
        StreetVertex nowhere = (StreetVertex) graph.addVertex(new StreetVertex("nowhere", GeometryUtils.makeLineString(between.x, between.y, between.x - 0.000001, between.y - 0.000001), "nowhere", 10000, false));
        StreetVertex nowhere2 = (StreetVertex) graph.addVertex(new StreetVertex("nowhere", GeometryUtils.makeLineString(between.x - 0.000001, between.y - 0.000001, between.x, between.y), "nowhere", 10000, true));

        
        for (int i = 0; i < nearPoints.length; ++i) {
            StreetVertex a = nearPoints[i];
            TurnEdge street = new TurnEdge(a, nowhere);
            graph.addEdge(a, nowhere, street);
            TurnEdge street2 = new TurnEdge(nowhere2, a);
            graph.addEdge(nowhere2, a, street2);
        }

        NetworkLinker nl = new NetworkLinker(graph);
        nl.createLinkage(true);
        
        assertTrue(graph.getIncoming(near_b.getLabel()).size() >= 2);
    }

    public void testBoardAlight() throws Exception {

        GraphVertex stop_a = graph.getGraphVertex("agency_A");
        GraphVertex stop_b = graph.getGraphVertex("agency_B");

        assertEquals(2, stop_a.getDegreeOut());
        assertEquals(4, stop_b.getDegreeOut());

        for (Edge e : stop_a.getOutgoing()) {
            assertTrue(e instanceof PatternBoard || e instanceof StreetTransitLink);
        }
        
        GraphVertex journey_a_1 = graph.getGraphVertex(stop_a.getOutgoing().iterator().next().getToVertex());

        assertEquals(1, journey_a_1.getDegreeIn());

        for (Edge e : journey_a_1.getOutgoing()) {
            if (e.getToVertex() instanceof TransitStop) {
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
        assertTrue(path.vertices.size() <= 9);
        long endTime = new GregorianCalendar(2009, 8, 7, 0, 0, 0).getTimeInMillis() + 40 * 60 * 1000;
        assertEquals(endTime, path.vertices.lastElement().state.getTime());

        spt = AStar.getShortestPathTree(graph, stop_a.getLabel(), stop_e.getLabel(), new State(
                new GregorianCalendar(2009, 8, 7, 0, 0, 0).getTimeInMillis()), options);

        path = spt.getPath(stop_e);
        assertNotNull(path);
        assertTrue(path.vertices.size() <= 10);
        endTime = new GregorianCalendar(2009, 8, 7, 0, 0, 0).getTimeInMillis() + 70 * 60 * 1000;
        assertEquals(endTime, path.vertices.lastElement().state.getTime());
    }

    /**
     * Test that useless dwell edges are in fact removed.
     */
    public void testDwellSimplification() {
        Vertex stop_f = graph.getVertex("agency_F");
        Vertex stop_h = graph.getVertex("agency_H");

        TraverseOptions options = new TraverseOptions();
        options.setGtfsContext(context);

        ShortestPathTree spt;
        GraphPath path;

        spt = AStar.getShortestPathTree(graph, stop_f.getLabel(), stop_h.getLabel(), new State(
                new GregorianCalendar(2009, 8, 18, 5, 0, 0).getTimeInMillis()), options);

        path = spt.getPath(stop_h);
        assertNotNull(path);
        assertEquals(5, path.vertices.size());
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
        spt = AStar.getShortestPathTree(graph, stop_g.getLabel(), stop_h.getLabel(), new State(
                new GregorianCalendar(2009, 8, 18, 23, 20, 0).getTimeInMillis()), options);

        path = spt.getPath(stop_h);
        assertNotNull(path);
        assertEquals(4, path.vertices.size());

        // Saturday morning
        long startTime = new GregorianCalendar(2009, 8, 19, 0, 5, 0).getTimeInMillis();

        spt = AStar.getShortestPathTree(graph, stop_g.getLabel(), stop_h.getLabel(), new State(
                startTime), options);

        path = spt.getPath(stop_h);
        assertNotNull(path);
        assertEquals(4, path.vertices.size());
        long endTime = path.vertices.lastElement().state.getTime();
        assertTrue(endTime < startTime + 1000 * 60 * 60);
    }

    public Edge getHopOut(Vertex v) {
        for (Edge e : graph.getOutgoing(v)) {
            if (e instanceof PatternBoard) {
                for (Edge f : graph.getOutgoing(e.getToVertex())) {
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
        assertTrue(geometry.getLength() > 0.009999);
        assertTrue(geometry.getLength() < 0.010001);

    }

    public void testShapeByDistance() throws Exception {
        Vertex stop_i = graph.getVertex("agency_I");
        Edge hop = getHopOut(stop_i);
        Geometry geometry = hop.getGeometry();
        assertTrue(geometry.getLength() > 1.0);
        assertTrue(geometry.getLength() < 2.0);
    }

    public void testPickupDropoff() throws Exception {
        Vertex stop_o = graph.getVertex("agency_O");
        Vertex stop_p = graph.getVertex("agency_P");
        int i = 0;
        for (@SuppressWarnings("unused") Edge e: graph.getOutgoing(stop_o)) {
            ++i;
        }
        assertTrue(i == 3);

        long startTime = new GregorianCalendar(2009, 8, 19, 12, 0, 0).getTimeInMillis();
        TraverseOptions options = new TraverseOptions(context);
        ShortestPathTree spt = AStar.getShortestPathTree(graph, stop_o, stop_p, new State(startTime), options );
        GraphPath path = spt.getPath(stop_p);
        assertNotNull(path);
        long endTime = new GregorianCalendar(2009, 8, 19, 12, 10, 0).getTimeInMillis();
        assertEquals(endTime, path.vertices.lastElement().state.getTime());

        startTime = new GregorianCalendar(2009, 8, 19, 12, 0, 1).getTimeInMillis();
        spt = AStar.getShortestPathTree(graph, stop_o, stop_p, new State(startTime), options );
        path = spt.getPath(stop_p);
        assertNotNull(path);
        endTime = new GregorianCalendar(2009, 8, 19, 15, 10, 0).getTimeInMillis();
        assertEquals(endTime, path.vertices.lastElement().state.getTime());
    }

    public void testTransfers() throws Exception {
        Vertex stop_k = graph.getVertex("agency_K");
        Vertex stop_n = graph.getVertex("agency_N");
        int transfers = 0;
        for (Edge e : graph.getOutgoing(stop_n)) {
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

        ShortestPathTree spt = AStar.getShortestPathTree(graph, stop_i.getLabel(), stop_k
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

        spt = AStar.getShortestPathTree(graph, stop_a.getLabel(), stop_b.getLabel(), new State(
                new GregorianCalendar(2009, 8, 0, 0, 0, 0).getTimeInMillis()), options );

        //a to b is bus only
        assertNull(spt.getPath(stop_b));
        
        options.modes = new TraverseModeSet("TRAINISH,BUSISH");
        spt = AStar.getShortestPathTree(graph, stop_a.getLabel(), stop_b.getLabel(), new State(
                new GregorianCalendar(2009, 8, 0, 0, 0, 0).getTimeInMillis()), options );

        assertNotNull(spt.getPath(stop_b));
    }
    
    public void testTimelessStops() throws Exception {
        Vertex stop_d = graph.getVertex("agency_D");
        Vertex stop_c = graph.getVertex("agency_C");
        TraverseOptions options = new TraverseOptions(context);
        ShortestPathTree spt = AStar.getShortestPathTree(graph, stop_d.getLabel(), stop_c.getLabel(), new State(
                new GregorianCalendar(2009, 8, 1, 10, 0, 0).getTimeInMillis()), options);

        GraphPath path = spt.getPath(stop_c);
        assertNotNull(path);
        State endState = path.vertices.lastElement().state;
        assertEquals(new GregorianCalendar(2009, 8, 1, 11, 0, 0).getTimeInMillis(), endState.getTime());
    }

    public void testWheelchairAccessible() throws Exception {
        Vertex near_a = graph.getVertex("near_a");
        Vertex near_b = graph.getVertex("near_b");
        Vertex near_c = graph.getVertex("near_c");
        Vertex near_d = graph.getVertex("near_d");

        TraverseOptions options = new TraverseOptions(context);
        options.wheelchairAccessible = true;

        ShortestPathTree spt;
        GraphPath path;
        // stop B is accessible, so there should be a path.
        spt = AStar.getShortestPathTree(graph, near_a, near_b, new State(new GregorianCalendar(
                2009, 8, 18, 0, 0, 0).getTimeInMillis()), options);

        path = spt.getPath(near_b);
        assertNotNull(path);

        // stop C is not accessible, so there should be no path.
        spt = AStar.getShortestPathTree(graph, near_a, near_c, new State(new GregorianCalendar(
                2009, 8, 18, 0, 0, 0).getTimeInMillis()), options);

        path = spt.getPath(near_c);
        assertNull(path);

        // from stop A to stop D would normally be trip 1.1 to trip 2.1, arriving at 00:30. But trip
        // 2 is not accessible, so we'll do 1.1 to 3.1, arriving at 01:00
        GregorianCalendar time = new GregorianCalendar(2009, 8, 18, 0, 0, 0);
        spt = AStar.getShortestPathTree(graph, near_a, near_d, new State(time
                .getTimeInMillis()), options);
        
        time.add(Calendar.HOUR, 1);
        time.add(Calendar.SECOND, 1); //for the StreetTransitLink
        path = spt.getPath(near_d);
        assertNotNull(path);
        assertEquals(time.getTimeInMillis(), path.vertices.lastElement().state.getTime());
    }

    public void testRunForTrain() {

        Vertex destination = graph.getVertex("agency_T");
        TraverseOptions wo = new TraverseOptions();
        wo.setGtfsContext(context);
        GregorianCalendar startTime = new GregorianCalendar(2009, 11, 2, 8, 30, 0);
        ShortestPathTree spt = AStar.getShortestPathTree(graph, "agency_Q", destination.getLabel(),
                new State(startTime.getTimeInMillis()), wo);
        GraphPath path = spt.getPath(destination);

        long endTime = path.vertices.lastElement().state.getTime();
        Calendar c = new GregorianCalendar();
        c.setTimeInMillis(endTime);
        assertTrue((endTime - startTime.getTimeInMillis()) / 1000 < 7200);
    }

    public void testFrequencies() {
        Vertex stop_u = graph.getVertex("agency_U");
        Vertex stop_v = graph.getVertex("agency_V");

        ShortestPathTree spt;
        GraphPath path;

        TraverseOptions options = new TraverseOptions(context);
        options.modes = new TraverseModeSet("TRANSIT");

        // U to V - original stop times - shouldn't be used
        spt = AStar.getShortestPathTree(graph, stop_u.getLabel(), stop_v.getLabel(), new State(
                new GregorianCalendar(2009, 8, 7, 0, 0, 0).getTimeInMillis()), options);
        path = spt.getPath(stop_v);
        assertNotNull(path);
        assertEquals(4, path.vertices.size());
        long endTime = new GregorianCalendar(2009, 8, 7, 6, 40, 0).getTimeInMillis();
        assertEquals(endTime, path.vertices.lastElement().state.getTime());

        // U to V - first frequency
        spt = AStar.getShortestPathTree(graph, stop_u.getLabel(), stop_v.getLabel(), new State(
                new GregorianCalendar(2009, 8, 7, 7, 0, 0).getTimeInMillis()), options);
        path = spt.getPath(stop_v);
        assertNotNull(path);
        assertEquals(4, path.vertices.size());
        endTime = new GregorianCalendar(2009, 8, 7, 7, 40, 0).getTimeInMillis();
        assertEquals(endTime, path.vertices.lastElement().state.getTime());

        // U to V - second frequency
        spt = AStar.getShortestPathTree(graph, stop_u.getLabel(), stop_v.getLabel(), new State(
                new GregorianCalendar(2009, 8, 7, 14, 0, 0).getTimeInMillis()), options);
        path = spt.getPath(stop_v);
        assertNotNull(path);
        assertEquals(4, path.vertices.size());
        endTime = new GregorianCalendar(2009, 8, 7, 14, 40, 0).getTimeInMillis();
        assertEquals(endTime, path.vertices.lastElement().state.getTime());
    }
    
    public void testFewestTransfers() {
        Vertex stop_c = graph.getVertex("agency_C");
        Vertex stop_d = graph.getVertex("agency_D");
        TraverseOptions options = new TraverseOptions(context);
        options.optimizeFor = OptimizeType.QUICK;
        ShortestPathTree spt = AStar.getShortestPathTree(graph, stop_c.getLabel(), stop_d.getLabel(), new State(
                new GregorianCalendar(2009, 8, 1, 16, 0, 0).getTimeInMillis()), options);

        //when optimizing for speed, take the fast two-bus path
        GraphPath path = spt.getPath(stop_d);
        assertNotNull(path);
        State endState = path.vertices.lastElement().state;
        assertEquals(new GregorianCalendar(2009, 8, 1, 16, 20, 0).getTimeInMillis(), endState.getTime());
        
        //when optimizing for fewest transfers, take the slow one-bus path
        options.optimizeFor = OptimizeType.TRANSFERS;
        spt = AStar.getShortestPathTree(graph, stop_c.getLabel(), stop_d.getLabel(), new State(
                new GregorianCalendar(2009, 8, 1, 16, 0, 0).getTimeInMillis()), options);

        path = spt.getPath(stop_d);
        assertNotNull(path);
        endState = path.vertices.lastElement().state;
        assertEquals(new GregorianCalendar(2009, 8, 1, 16, 50, 0).getTimeInMillis(), endState.getTime());

    }

    public void testPathways() throws Exception {

        Vertex entrance = graph.getVertex("agency_entrance_a");
        assertNotNull(entrance);
        Vertex stop = graph.getVertex("agency_A");
        assertNotNull(stop);

        TraverseOptions options = new TraverseOptions(context);
        ShortestPathTree spt = AStar.getShortestPathTree(graph, entrance, stop, new State(
                new GregorianCalendar(2009, 8, 1, 16, 0, 0).getTimeInMillis()), options);
        
        GraphPath path = spt.getPath(stop);
        assertNotNull(path);
        State endState = path.vertices.lastElement().state;
        assertEquals(new GregorianCalendar(2009, 8, 1, 16, 0, 34).getTimeInMillis(), endState.getTime());
    }
}