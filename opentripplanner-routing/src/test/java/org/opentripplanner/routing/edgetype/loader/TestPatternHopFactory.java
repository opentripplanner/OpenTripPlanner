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
import java.util.Calendar;
import java.util.GregorianCalendar;

import junit.framework.TestCase;

import org.opentripplanner.ConstantsForTests;
import org.opentripplanner.common.geometry.GeometryUtils;
import org.opentripplanner.gtfs.GtfsContext;
import org.opentripplanner.gtfs.GtfsLibrary;
import org.opentripplanner.routing.algorithm.AStar;
import org.opentripplanner.routing.core.DirectEdge;
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
import org.opentripplanner.routing.edgetype.TransferEdge;
import org.opentripplanner.routing.edgetype.TurnEdge;
import org.opentripplanner.routing.edgetype.factory.GTFSPatternHopFactory;
import org.opentripplanner.routing.spt.GraphPath;
import org.opentripplanner.routing.spt.SPTEdge;
import org.opentripplanner.routing.spt.ShortestPathTree;
import org.opentripplanner.routing.core.TransitStop;

import com.vividsolutions.jts.geom.Geometry;

public class TestPatternHopFactory extends TestCase {

    private Graph graph;

    private GtfsContext context;

    public void setUp() throws Exception {

        context = GtfsLibrary.readGtfs(new File(ConstantsForTests.FAKE_GTFS));
        graph = new Graph();

        GTFSPatternHopFactory factory = new GTFSPatternHopFactory(context);
        factory.run(graph);

        String[] stops = {"agency_A", "agency_B", "agency_C", "agency_D", "agency_E"};
        for (int i = 0; i < stops.length; ++i) {
            Vertex stop = graph.getVertex(stops[i]);
            
            StreetVertex front = (StreetVertex) graph.addVertex(new StreetVertex("near_" + stop.getStopId(), GeometryUtils.makeLineString(stop.getX() + 0.0001, stop.getY() + 0.0001, stop.getX() - 0.0001, stop.getY() - 0.0001), "near " + stop.getStopId(), 100, false));
            StreetVertex back = (StreetVertex) graph.addVertex(new StreetVertex("near_" + stop.getStopId(), GeometryUtils.makeLineString(stop.getX() - 0.0001, stop.getY() - 0.0001, stop.getX() + 0.0001, stop.getY() + 0.0001), "near " + stop.getStopId(), 100, true));
            
            TurnEdge street = new TurnEdge(front, back);
            graph.addEdge(street);
            
            TurnEdge street2 = new TurnEdge(back, front);
            graph.addEdge(street2);
        }

        NetworkLinker nl = new NetworkLinker(graph);
        nl.createLinkage();
    }

    public void testBoardAlight() throws Exception {
        GraphVertex stop_a_depart = graph.getGraphVertex("agency_A_depart");
        GraphVertex stop_b_depart = graph.getGraphVertex("agency_B_depart");
        
        assertEquals(1, stop_a_depart.getDegreeOut());
        assertEquals(3, stop_b_depart.getDegreeOut());

        for (Edge e : stop_a_depart.getOutgoing()) {
            assertTrue(e instanceof PatternBoard);
        }
        
        PatternBoard pb = (PatternBoard) stop_a_depart.getOutgoing().iterator().next();
        GraphVertex journey_a_1 = graph.getGraphVertex(pb.getToVertex());

        assertEquals(1, journey_a_1.getDegreeIn());

        for (DirectEdge e : filter(journey_a_1.getOutgoing(), DirectEdge.class)) {
            if (e.getToVertex() instanceof TransitStop) {
                assertEquals(Alight.class, e.getClass());
            } else {
                assertEquals(PatternHop.class, e.getClass());
            }
        }
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

        // A to D (change at C)
        spt = AStar.getShortestPathTree(graph, stop_a.getLabel(), stop_d.getLabel(), new State(
                new GregorianCalendar(2009, 8, 7, 0, 0, 0).getTimeInMillis()), options);

        path = spt.getPath(stop_d);
        assertNotNull(path);
        assertTrue(path.vertices.size() == 11);
        long endTime = new GregorianCalendar(2009, 8, 7, 0, 0, 0).getTimeInMillis() + 40 * 60 * 1000;
        assertEquals(endTime, path.vertices.lastElement().state.getTime());

        spt = AStar.getShortestPathTree(graph, stop_a.getLabel(), stop_e.getLabel(), new State(
                new GregorianCalendar(2009, 8, 7, 0, 0, 0).getTimeInMillis()), options);

        //A to E (change at C)
        path = spt.getPath(stop_e);
        assertNotNull(path);
        assertTrue(path.vertices.size() == 12);
        endTime = new GregorianCalendar(2009, 8, 7, 0, 0, 0).getTimeInMillis() + 70 * 60 * 1000;
        assertEquals(endTime, path.vertices.lastElement().state.getTime());
    }

    /**
     * Test that useless dwell edges are in fact removed.
     */
    public void testDwellSimplification() {
        Vertex stop_f = graph.getVertex("agency_F_depart");
        Vertex stop_h = graph.getVertex("agency_H_arrive");

        TraverseOptions options = new TraverseOptions();
        options.setGtfsContext(context);

        ShortestPathTree spt;
        GraphPath path;

        spt = AStar.getShortestPathTree(graph, stop_f, stop_h, new State(
                new GregorianCalendar(2009, 8, 18, 5, 0, 0).getTimeInMillis()), options);

        path = spt.getPath(stop_h);
        assertNotNull(path);
        assertEquals(5, path.vertices.size());
    }

    public void testRoutingOverMidnight() throws Exception {
        // this route only runs on weekdays
        Vertex stop_g = graph.getVertex("agency_G_depart");
        Vertex stop_h = graph.getVertex("agency_H_arrive");

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

    public PatternHop getHopOut(Vertex v) {
        for (PatternBoard e : filter(graph.getOutgoing(v), PatternBoard.class)) {
            for (PatternHop f : filter(graph.getOutgoing(e.getToVertex()), PatternHop.class)) {
                return (PatternHop) f;
            }
        }
        return null;
    }

    public void testShapeByLocation() throws Exception {
        Vertex stop_g = graph.getVertex("agency_G_depart");
        PatternHop hop = getHopOut(stop_g);
        Geometry geometry = hop.getGeometry();
        assertTrue(geometry.getLength() > 1.0);

        Vertex stop_a = graph.getVertex("agency_A_depart");
        hop = getHopOut(stop_a);
        geometry = hop.getGeometry();
        assertTrue(geometry.getLength() > 0.009999);
        assertTrue(geometry.getLength() < 0.010001);

    }

    public void testShapeByDistance() throws Exception {
        Vertex stop_i = graph.getVertex("agency_I_depart");
        PatternHop hop = getHopOut(stop_i);
        Geometry geometry = hop.getGeometry();
        assertTrue(geometry.getLength() > 1.0);
        assertTrue(geometry.getLength() < 2.0);
    }

    public void testPickupDropoff() throws Exception {
        Vertex stop_o = graph.getVertex("agency_O_depart");
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
        Vertex stop_k = graph.getVertex("agency_K_depart");
        Vertex stop_n = graph.getVertex("agency_N_arrive");
        int transfers = 0;
        for (TransferEdge e : filter(graph.getOutgoing(stop_n), TransferEdge.class)) {
            assertEquals(e.getToVertex(), stop_k);
            transfers += 1;
        }
        assertTrue(transfers > 0);
    }

    public void testInterlining() throws Exception {
        Vertex stop_i = graph.getVertex("agency_I_depart");
        Vertex stop_k = graph.getVertex("agency_K_arrive");

        long startTime = new GregorianCalendar(2009, 8, 19, 12, 0, 0).getTimeInMillis();
        TraverseOptions options = new TraverseOptions(context);

        ShortestPathTree spt = AStar.getShortestPathTree(graph, stop_i, stop_k, 
                new State(startTime), options);
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
        
        options.setArriveBy(true);
        spt = AStar.getShortestPathTreeBack(graph, stop_i, stop_k, 
                new State(startTime), options);
        path = spt.getPath(stop_i);
        path.reverse();
        num_alights = 0;
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
        Vertex stop_a = graph.getVertex("agency_A_depart");
        Vertex stop_b = graph.getVertex("agency_B_arrive");

        ShortestPathTree spt;

        TraverseOptions options = new TraverseOptions(context);
        options.setModes(new TraverseModeSet("TRAINISH"));

        spt = AStar.getShortestPathTree(graph, stop_a.getLabel(), stop_b.getLabel(), new State(
                new GregorianCalendar(2009, 8, 0, 0, 0, 0).getTimeInMillis()), options );

        //a to b is bus only
        assertNull(spt.getPath(stop_b));
        
        options.setModes(new TraverseModeSet("TRAINISH,BUSISH"));
        spt = AStar.getShortestPathTree(graph, stop_a.getLabel(), stop_b.getLabel(), new State(
                new GregorianCalendar(2009, 8, 0, 0, 0, 0).getTimeInMillis()), options );

        assertNotNull(spt.getPath(stop_b));
    }
    
    public void testTimelessStops() throws Exception {
        Vertex stop_d = graph.getVertex("agency_D");
        Vertex stop_c = graph.getVertex("agency_C");
        TraverseOptions options = new TraverseOptions(context);
        ShortestPathTree spt = AStar.getShortestPathTree(graph, stop_d.getLabel(),stop_c.getLabel(), new State(
                new GregorianCalendar(2009, 8, 1, 10, 0, 0).getTimeInMillis()), options);

        GraphPath path = spt.getPath(stop_c);
        assertNotNull(path);
        State endState = path.vertices.lastElement().state;
        assertEquals(new GregorianCalendar(2009, 8, 1, 11, 0, 0).getTimeInMillis(), endState.getTime());
    }

    public void testWheelchairAccessible() throws Exception {
        Vertex near_a = graph.getVertex("near_A");
        Vertex near_b = graph.getVertex("near_B");
        Vertex near_c = graph.getVertex("near_C");

        Vertex stop_d = graph.getVertex("agency_D");
        Vertex split_d = null;
        for (StreetTransitLink e : filter(graph.getOutgoing(stop_d), StreetTransitLink.class)) {
            split_d = e.getToVertex();
        }
        
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
        spt = AStar.getShortestPathTree(graph, near_a, split_d, new State(time
               .getTimeInMillis()), options);
        
        time.add(Calendar.HOUR, 1);
        time.add(Calendar.SECOND, 1); //for the StreetTransitLink
        path = spt.getPath(split_d);
        assertNotNull(path);
        assertEquals(time.getTimeInMillis(), path.vertices.lastElement().state.getTime());
    }

    public void testRunForTrain() {
        /** This is the notorious Byrd bug: we're going from Q to T at 8:30.  
         *  There's a trip from S to T at 8:50 and a second one at 9:50.  
         *  To get to S by 8:50, we need to take trip 12.1 from Q to R, and 13.1
         *  from R to S.  If we take the direct-but-slower 11.1, we'll miss
         *  the 8:50 and have to catch the 9:50.
         */
        
        Vertex destination = graph.getVertex("agency_T_depart");
        TraverseOptions wo = new TraverseOptions();
        wo.setGtfsContext(context);
        GregorianCalendar startTime = new GregorianCalendar(2009, 11, 2, 8, 30, 0);
        ShortestPathTree spt = AStar.getShortestPathTree(graph, "agency_Q_arrive", destination.getLabel(),
                new State(startTime.getTimeInMillis()), wo);
        GraphPath path = spt.getPath(destination);

        long endTime = path.vertices.lastElement().state.getTime();
        Calendar c = new GregorianCalendar();
        c.setTimeInMillis(endTime);
        assertTrue((endTime - startTime.getTimeInMillis()) / 1000 < 7200);
    }

    public void testFrequencies() {
        Vertex stop_u = graph.getVertex("agency_U_depart");
        Vertex stop_v = graph.getVertex("agency_V_arrive");

        ShortestPathTree spt;
        GraphPath path;

        TraverseOptions options = new TraverseOptions(context);
        options.setModes(new TraverseModeSet("TRANSIT"));

        // U to V - original stop times - shouldn't be used
        spt = AStar.getShortestPathTree(graph, stop_u, stop_v, new State(
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