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

import static org.opentripplanner.common.IterableLibrary.filter;

import java.io.File;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.TimeZone;

import junit.framework.TestCase;

import org.onebusaway.gtfs.model.calendar.CalendarServiceData;
import org.opentripplanner.ConstantsForTests;
import org.opentripplanner.common.geometry.GeometryUtils;
import org.opentripplanner.gbannotation.GraphBuilderAnnotation;
import org.opentripplanner.gbannotation.NegativeHopTime;
import org.opentripplanner.gtfs.GtfsContext;
import org.opentripplanner.gtfs.GtfsLibrary;
import org.opentripplanner.routing.algorithm.GenericAStar;
import org.opentripplanner.routing.core.OptimizeType;
import org.opentripplanner.routing.core.RoutingRequest;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.core.StopTransfer;
import org.opentripplanner.routing.core.TransferTable;
import org.opentripplanner.routing.core.TraverseModeSet;
import org.opentripplanner.routing.edgetype.FrequencyBasedTripPattern;
import org.opentripplanner.routing.edgetype.FrequencyBoard;
import org.opentripplanner.routing.edgetype.PatternDwell;
import org.opentripplanner.routing.edgetype.PatternHop;
import org.opentripplanner.routing.edgetype.PlainStreetEdge;
import org.opentripplanner.routing.edgetype.SimpleEdge;
import org.opentripplanner.routing.edgetype.StreetTransitLink;
import org.opentripplanner.routing.edgetype.StreetTraversalPermission;
import org.opentripplanner.routing.edgetype.TransitBoardAlight;
import org.opentripplanner.routing.edgetype.factory.GTFSPatternHopFactory;
import org.opentripplanner.routing.graph.Edge;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.routing.spt.GraphPath;
import org.opentripplanner.routing.spt.ShortestPathTree;
import org.opentripplanner.routing.vertextype.IntersectionVertex;
import org.opentripplanner.routing.vertextype.TransitStop;
import org.opentripplanner.routing.vertextype.TransitStopArrive;
import org.opentripplanner.routing.vertextype.TransitStopDepart;
import org.opentripplanner.util.TestUtils;

import com.vividsolutions.jts.geom.Geometry;

public class TestPatternHopFactory extends TestCase {

    private Graph graph;
    private GenericAStar aStar = new GenericAStar();
    private GtfsContext context;

    public void setUp() throws Exception {

        context = GtfsLibrary.readGtfs(new File(ConstantsForTests.FAKE_GTFS));
        graph = new Graph();

        GTFSPatternHopFactory factory = new GTFSPatternHopFactory(context);
        factory.run(graph);
        graph.putService(CalendarServiceData.class, GtfsLibrary.createCalendarServiceData(context.getDao()));
        
        String[] stops = {"agency_A", "agency_B", "agency_C", "agency_D", "agency_E"};
        for (int i = 0; i < stops.length; ++i) {
            TransitStop stop = (TransitStop) (graph.getVertex(stops[i]));
            
            IntersectionVertex front = new IntersectionVertex(graph, "near_1_" + stop.getStopId(), stop.getX() + 0.0001, stop.getY() + 0.0001);
            IntersectionVertex back =  new IntersectionVertex(graph, "near_2_" + stop.getStopId(), stop.getX() - 0.0001, stop.getY() - 0.0001);
            
            PlainStreetEdge street1 = new PlainStreetEdge(front, back, GeometryUtils.makeLineString(stop.getX() + 0.0001, stop.getY() + 0.0001, stop.getX() - 0.0001, stop.getY() - 0.0001), "street", 100, StreetTraversalPermission.ALL, false);
            PlainStreetEdge street2 = new PlainStreetEdge(back, front, GeometryUtils.makeLineString(stop.getX() - 0.0001, stop.getY() - 0.0001, stop.getX() + 0.0001, stop.getY() + 0.0001), "street", 100, StreetTraversalPermission.ALL, true);
        }

        NetworkLinker nl = new NetworkLinker(graph);
        nl.createLinkage();
    }

    public void testAnnotation() {
        boolean found = false;
        for (GraphBuilderAnnotation annotation : graph.getBuilderAnnotations()) {
            if (annotation instanceof NegativeHopTime) {
                NegativeHopTime nht = (NegativeHopTime) annotation;
                assertTrue(nht.st0.getDepartureTime() > nht.st1.getArrivalTime());
                found = true;
            }
        }
        assertTrue(found);
    }
    
    public void testBoardAlight() throws Exception {
        Vertex stop_a_depart = graph.getVertex("agency_A_depart");
        Vertex stop_b_depart = graph.getVertex("agency_B_depart");
        
        assertEquals(1, stop_a_depart.getDegreeOut());
        assertEquals(3, stop_b_depart.getDegreeOut());

        for (Edge e : stop_a_depart.getOutgoing()) {
            assertEquals(TransitBoardAlight.class, e.getClass());
            assertTrue(((TransitBoardAlight) e).isBoarding());
        }
        
        TransitBoardAlight pb = (TransitBoardAlight) stop_a_depart.getOutgoing().iterator().next();
        Vertex journey_a_1 = pb.getToVertex();

        assertEquals(1, journey_a_1.getDegreeIn());

        for (Edge e : journey_a_1.getOutgoing()) {
            if (e.getToVertex() instanceof TransitStop) {
                assertEquals(TransitBoardAlight.class, e.getClass());
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

        RoutingRequest options = new RoutingRequest();
        // test feed is designed for instantaneous transfers
        options.setTransferSlack(0);

        long startTime = TestUtils.dateInSeconds("America/New_York", 2009, 8, 7, 0, 0, 0);
        options.dateTime = startTime;

        ShortestPathTree spt;
        GraphPath path;

        // A to B
        options.setRoutingContext(graph, stop_a, stop_b);
        spt = aStar.getShortestPathTree(options);

        path = spt.getPath(stop_b, false);
        assertNotNull(path);
        assertEquals(6, path.states.size());

        // A to C
        options.setRoutingContext(graph, stop_a, stop_c);
        spt = aStar.getShortestPathTree(options);

        path = spt.getPath(stop_c, false);
        assertNotNull(path);
        assertEquals(8, path.states.size());

        // A to D (change at C)
        options.setRoutingContext(graph, stop_a, stop_d);
        spt = aStar.getShortestPathTree(options);

        path = spt.getPath(stop_d, false);
        assertNotNull(path);
        // there are two paths of different lengths 
        // both arrive at 40 minutes after midnight
        //assertTrue(path.states.size() == 13);
        long endTime = startTime + 40 * 60;
        assertEquals(endTime, path.getEndTime());

        //A to E (change at C)
        options.setRoutingContext(graph, stop_a, stop_e);
        spt = aStar.getShortestPathTree(options);
        path = spt.getPath(stop_e, false);
        assertNotNull(path);
        assertTrue(path.states.size() == 14);
        endTime = startTime + 70 * 60;
        assertEquals(endTime, path.getEndTime());
    }

    /**
     * Test that useless dwell edges are in fact removed.
     */
    public void testDwellSimplification() {
        Vertex stop_f = graph.getVertex("agency_F_depart");
        Vertex stop_h = graph.getVertex("agency_H_arrive");

        RoutingRequest options = new RoutingRequest();
        options.dateTime = TestUtils.dateInSeconds("America/New_York", 2009, 8, 18, 5, 0, 0);
        options.setRoutingContext(graph, stop_f, stop_h);

        ShortestPathTree spt = aStar.getShortestPathTree(options);
        GraphPath path = spt.getPath(stop_h, false);
        assertNotNull(path);
        assertEquals(5, path.states.size());
    }

    public void testRoutingOverMidnight() throws Exception {
        // this route only runs on weekdays
        Vertex stop_g = graph.getVertex("agency_G_depart");
        Vertex stop_h = graph.getVertex("agency_H_arrive");

        ShortestPathTree spt;
        GraphPath path;
        RoutingRequest options = new RoutingRequest();

        // Friday evening
        options.dateTime = TestUtils.dateInSeconds("America/New_York", 2009, 8, 18, 23, 20, 0); 
        options.setRoutingContext(graph, stop_g, stop_h);
        spt = aStar.getShortestPathTree(options);

        path = spt.getPath(stop_h, false);
        assertNotNull(path);
        assertEquals(4, path.states.size());

        // Saturday morning
        long startTime = TestUtils.dateInSeconds("America/New_York", 2009, 8, 19, 0, 5, 0);
        options.dateTime = startTime;
        options.setRoutingContext(graph, stop_g.getLabel(), stop_h.getLabel());
        spt = aStar.getShortestPathTree(options);

        path = spt.getPath(stop_h, false);
        assertNotNull(path);
        assertEquals(4, path.states.size());
        long endTime = path.getEndTime();
        assertTrue(endTime < startTime + 60 * 60);
    }

    public PatternHop getHopOut(Vertex v) {
        for (TransitBoardAlight e : filter(v.getOutgoing(), TransitBoardAlight.class)) {
            if (!e.isBoarding())
                continue;
            
            for (PatternHop f : filter(e.getToVertex().getOutgoing(), PatternHop.class)) {
                return f;
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
        for (@SuppressWarnings("unused") Edge e: stop_o.getOutgoing()) {
            ++i;
        }
        assertTrue(i == 3);

        long startTime = TestUtils.dateInSeconds("America/New_York", 2009, 8, 19, 12, 0, 0);
        RoutingRequest options = new RoutingRequest();
        options.dateTime = startTime;
        options.setRoutingContext(graph, stop_o, stop_p);
        ShortestPathTree spt = aStar.getShortestPathTree(options);
        GraphPath path = spt.getPath(stop_p, false);
        assertNotNull(path);
        long endTime = TestUtils.dateInSeconds("America/New_York", 2009, 8, 19, 12, 10, 0);
        assertEquals(endTime, path.getEndTime());

        startTime = TestUtils.dateInSeconds("America/New_York", 2009, 8, 19, 12, 0, 1);
        options.dateTime = startTime;
        options.setRoutingContext(graph, stop_o, stop_p);
        spt = aStar.getShortestPathTree(options);
        path = spt.getPath(stop_p, false);
        assertNotNull(path);
        endTime =  TestUtils.dateInSeconds("America/New_York", 2009, 8, 19, 15, 10, 0);
        assertEquals(endTime, path.getEndTime());
    }

    public void testTransfers() throws Exception {
        TransferTable transferTable = graph.getTransferTable();
        assertTrue(transferTable.hasPreferredTransfers());
        assertTrue(transferTable.getFirstSpecificTransferTime(
                ((TransitStopArrive)graph.getVertex("agency_K_arrive")).getStop(), 
                ((TransitStopDepart)graph.getVertex("agency_N_depart")).getStop()
                ) == StopTransfer.PREFERRED_TRANSFER);
        
        Vertex e_arrive = graph.getVertex("agency_E_arrive");
        Vertex f_depart = graph.getVertex("agency_F_depart");
        Edge edge = new SimpleEdge(e_arrive, f_depart, 10000, 10000);
        
        long startTime = TestUtils.dateInSeconds("America/New_York", 2009, 8, 18, 0, 50, 0);
        Vertex stop_b = graph.getVertex("agency_B_depart");
        Vertex stop_g = graph.getVertex("agency_G_arrive");
        RoutingRequest options = new RoutingRequest();
        options.dateTime = startTime;
        options.setRoutingContext(graph, stop_b, stop_g);
        ShortestPathTree spt = aStar.getShortestPathTree(options);
        
        GraphPath path = spt.getPath(stop_g, false);
        assertNotNull(path);
        
        assertTrue("expected to use much later trip due to min transfer time", path.getEndTime() - startTime > 4.5 * 60 * 60);
        
        /* cleanup */
        e_arrive.removeOutgoing(edge);
        f_depart.removeIncoming(edge);
    }

    public void testInterlining() throws Exception {
        Vertex stop_i = graph.getVertex("agency_I_depart");
        Vertex stop_k = graph.getVertex("agency_K_arrive");

        long startTime = TestUtils.dateInSeconds("America/New_York", 2009, 8, 19, 12, 0, 0);
        RoutingRequest options = new RoutingRequest();
        options.dateTime = startTime;
        options.setRoutingContext(graph, stop_i, stop_k);
        ShortestPathTree spt = aStar.getShortestPathTree(options);
        
        GraphPath path = spt.getPath(stop_k, false);
        int num_alights = 0;
        for (State s : path.states) {
            if (s.getBackEdge() instanceof TransitBoardAlight
                && !((TransitBoardAlight) s.getBackEdge()).isBoarding()) {
                num_alights += 1;
            }
            if (s.getBackEdge() instanceof PatternDwell) {
                assertEquals(10 * 60, s.getTimeDeltaSeconds());
            }
        }
        assertEquals(1, num_alights);
        
        options.setArriveBy(true);
        options.setRoutingContext(graph, stop_i, stop_k);
        spt = aStar.getShortestPathTree(options);
        path = spt.getPath(stop_i, false);
//        path.reverse();
        num_alights = 0;
        for (State s : path.states) {
            if (s.getBackEdge() instanceof TransitBoardAlight
                && !((TransitBoardAlight) s.getBackEdge()).isBoarding()) {
                num_alights += 1;
            }
            if (s.getBackEdge() instanceof PatternDwell) {
                assertEquals(10 * 60, s.getTimeDeltaSeconds());
            }
        }
        assertEquals(1, num_alights);
    }
    
    public void testTraverseMode() throws Exception {
        Vertex stop_a = graph.getVertex("agency_A_depart");
        Vertex stop_b = graph.getVertex("agency_B_arrive");

        ShortestPathTree spt;

        RoutingRequest options = new RoutingRequest();
        options.setModes(new TraverseModeSet("TRAINISH"));
        options.dateTime = TestUtils.dateInSeconds("America/New_York", 2009, 8, 0, 0, 0, 0);
        options.setRoutingContext(graph, stop_a, stop_b);
        spt = aStar.getShortestPathTree(options );

        //a to b is bus only
        assertNull(spt.getPath(stop_b, false));
        
        options.setModes(new TraverseModeSet("TRAINISH,BUSISH"));
        spt = aStar.getShortestPathTree(options);

        assertNotNull(spt.getPath(stop_b, false));
    }
    
    public void testTimelessStops() throws Exception {
        Vertex stop_d = graph.getVertex("agency_D");
        Vertex stop_c = graph.getVertex("agency_C");
        RoutingRequest options = new RoutingRequest();
        options.dateTime = TestUtils.dateInSeconds("America/New_York", 2009, 8, 1, 10, 0, 0);
        options.setRoutingContext(graph, stop_d, stop_c);
        ShortestPathTree spt = aStar.getShortestPathTree(options);

        GraphPath path = spt.getPath(stop_c, false);
        assertNotNull(path);
        assertEquals(TestUtils.dateInSeconds("America/New_York", 2009, 8, 1, 11, 0, 0), path.getEndTime());
    }

    public void testTripBikesAllowed() throws Exception {
        Vertex stop_a = graph.getVertex("agency_A");
        Vertex stop_b = graph.getVertex("agency_B");
        Vertex stop_c = graph.getVertex("agency_C");
        Vertex stop_d = graph.getVertex("agency_D");

        RoutingRequest options = new RoutingRequest();
        options.getModes().setWalk(false);
        options.getModes().setBicycle(true);
        options.getModes().setTransit(true);
        options.dateTime = TestUtils.dateInSeconds("America/New_York", 2009, 8, 18, 0, 0, 0);
        options.setRoutingContext(graph, stop_a, stop_b);

        ShortestPathTree spt;
        GraphPath path;

        // route: bikes allowed, trip: no value
        spt = aStar.getShortestPathTree(options);

        path = spt.getPath(stop_b, false);
        assertNotNull(path);

        // route: bikes allowed, trip: bikes not allowed
        options.setRoutingContext(graph, stop_d, stop_c);
        spt = aStar.getShortestPathTree(options);

        path = spt.getPath(stop_c, false);
        assertNull(path);

        // route: bikes not allowed, trip: bikes allowed
        options.setRoutingContext(graph, stop_c, stop_d);
        spt = aStar.getShortestPathTree(options);

        path = spt.getPath(stop_d, false);
        assertNotNull(path);
    }

    public void testWheelchairAccessible() throws Exception {
        Vertex near_a = graph.getVertex("near_1_agency_A");
        Vertex near_b = graph.getVertex("near_1_agency_B");
        Vertex near_c = graph.getVertex("near_1_agency_C");

        Vertex stop_d = graph.getVertex("agency_D");
        Vertex split_d = null;
        for (StreetTransitLink e : filter(stop_d.getOutgoing(), StreetTransitLink.class)) {
            split_d = e.getToVertex();
        }
        
        RoutingRequest options = new RoutingRequest();
        options.wheelchairAccessible = true;
        options.dateTime = TestUtils.dateInSeconds("America/New_York", 2009, 8, 18, 0, 0, 0);

        ShortestPathTree spt;
        GraphPath path;

        // stop B is accessible, so there should be a path.
        options.setRoutingContext(graph, near_a, near_b);
        spt = aStar.getShortestPathTree(options);

        path = spt.getPath(near_b, false);
        assertNotNull(path);

        // stop C is not accessible, so there should be no path.
        options.setRoutingContext(graph, near_a, near_c);
        spt = aStar.getShortestPathTree(options);

        path = spt.getPath(near_c, false);
        assertNull(path);

        // from stop A to stop D would normally be trip 1.1 to trip 2.1, arriving at 00:30. But trip
        // 2 is not accessible, so we'll do 1.1 to 3.1, arriving at 01:00
        GregorianCalendar time = new GregorianCalendar(2009, 8, 18, 0, 0, 0);
        time.setTimeZone(TimeZone.getTimeZone("America/New_York"));
        options.dateTime = TestUtils.toSeconds(time);
        options.setRoutingContext(graph, near_a, split_d);
        spt = aStar.getShortestPathTree(options);
        
        time.add(Calendar.HOUR, 1);
        time.add(Calendar.SECOND, 1); //for the StreetTransitLink
        path = spt.getPath(split_d, false);
        assertNotNull(path);
        assertEquals(TestUtils.toSeconds(time), path.getEndTime());
    }

    public void testRunForTrain() {
        /** This is the notorious Byrd bug: we're going from Q to T at 8:30.  
         *  There's a trip from S to T at 8:50 and a second one at 9:50.  
         *  To get to S by 8:50, we need to take trip 12.1 from Q to R, and 13.1
         *  from R to S.  If we take the direct-but-slower 11.1, we'll miss
         *  the 8:50 and have to catch the 9:50.
         */
        Vertex destination = graph.getVertex("agency_T");
        RoutingRequest options = new RoutingRequest();
        // test is designed such that transfers must be instantaneous
        options.setTransferSlack(0);
        GregorianCalendar startTime = new GregorianCalendar(2009, 11, 2, 8, 30, 0);
        startTime.setTimeZone(TimeZone.getTimeZone("America/New_York"));
        options.dateTime = TestUtils.toSeconds(startTime);
        options.setRoutingContext(graph, "agency_Q", destination.getLabel());
        ShortestPathTree spt = aStar.getShortestPathTree(options);
        GraphPath path = spt.getPath(destination, false);

        // TODO this is wrong (milliseconds)
//        long endTime = path.getEndTime();
//        Calendar c = new GregorianCalendar();
//        c.setTimeInMillis(endTime);
//        assertTrue(endTime - startTime.getTimeInMillis() < 7200);
    }

    public void testFrequencies() {
        Vertex stop_u = graph.getVertex("agency_U_depart");
        Vertex stop_v = graph.getVertex("agency_V_arrive");

        ShortestPathTree spt;
        GraphPath path;

        RoutingRequest options = new RoutingRequest();
        options.setModes(new TraverseModeSet("TRANSIT"));
        options.dateTime = TestUtils.dateInSeconds("America/New_York", 2009, 8, 7, 0, 0, 0);
        options.setRoutingContext(graph, stop_u, stop_v);
        
        // U to V - original stop times - shouldn't be used
        spt = aStar.getShortestPathTree(options);
        path = spt.getPath(stop_v, false);
        assertNotNull(path);
        assertEquals(4, path.states.size());
        long endTime = TestUtils.dateInSeconds("America/New_York", 2009, 8, 7, 6, 40, 0);
        assertEquals(endTime, path.getEndTime());

        // U to V - first frequency
        options.dateTime = TestUtils.dateInSeconds("America/New_York", 2009, 8, 7, 7, 0, 0);
        options.setRoutingContext(graph, stop_u, stop_v);
        spt = aStar.getShortestPathTree(options);
        path = spt.getPath(stop_v, false);
        assertNotNull(path);
        assertEquals(4, path.states.size());
        endTime = TestUtils.dateInSeconds("America/New_York", 2009, 8, 7, 7, 40, 0);
        assertEquals(endTime, path.getEndTime());

        // U to V - second frequency
        options.dateTime = TestUtils.dateInSeconds("America/New_York", 2009, 8, 7, 14, 0, 0);
        options.setRoutingContext(graph, stop_u, stop_v);
        spt = aStar.getShortestPathTree(options);
        path = spt.getPath(stop_v, false);
        assertNotNull(path);
        assertEquals(4, path.states.size());
        endTime = TestUtils.dateInSeconds("America/New_York", 2009, 8, 7, 14, 40, 0);
        assertEquals(endTime, path.getEndTime());
        
        boolean boarded = false;
        for (FrequencyBoard e : filter(stop_u.getOutgoing(), FrequencyBoard.class)) {
            boarded = true;
            FrequencyBoard board = (FrequencyBoard) e;
            FrequencyBasedTripPattern pattern = board.getPattern();
            int previousArrivalTime = pattern.getPreviousArrivalTime(0, 0, false, false, false);
            assertTrue(previousArrivalTime < 0);
            
            previousArrivalTime = pattern.getPreviousArrivalTime(0, 60*60*7-1, false, false, false);
            assertEquals(60*60*6, previousArrivalTime);
            
            previousArrivalTime = pattern.getPreviousArrivalTime(0, 60*60*11, false, false, false);
            assertEquals(60*60*10, previousArrivalTime);
            
            previousArrivalTime = pattern.getPreviousArrivalTime(0, 60*60*18, false, false, false);
            assertEquals(60*60*16, previousArrivalTime);
        }
        assertTrue(boarded);
        
    }
    
    public void testFewestTransfers() {
        Vertex stop_c = graph.getVertex("agency_C");
        Vertex stop_d = graph.getVertex("agency_D");
        RoutingRequest options = new RoutingRequest();
        options.optimize = OptimizeType.QUICK;
        options.dateTime = TestUtils.dateInSeconds("America/New_York", 2009, 8, 1, 16, 0, 0);
        options.setRoutingContext(graph, stop_c, stop_d);  
                
        ShortestPathTree spt = aStar.getShortestPathTree(options);

        //when optimizing for speed, take the fast two-bus path
        GraphPath path = spt.getPath(stop_d, false);
        assertNotNull(path);
        assertEquals(TestUtils.dateInSeconds("America/New_York", 2009, 8, 1, 16, 20, 0), path.getEndTime());
        
        //when optimizing for fewest transfers, take the slow one-bus path
        options.transferPenalty = 1800;
        spt = aStar.getShortestPathTree(options);

        path = spt.getPath(stop_d, false);
        assertNotNull(path);
        assertEquals(TestUtils.dateInSeconds("America/New_York", 2009, 8, 1, 16, 50, 0), path.getEndTime());

    }

    public void testPathways() throws Exception {

        Vertex entrance = graph.getVertex("agency_entrance_a");
        assertNotNull(entrance);
        Vertex stop = graph.getVertex("agency_A");
        assertNotNull(stop);

        RoutingRequest options = new RoutingRequest();
        options.dateTime = TestUtils.dateInSeconds("America/New_York", 2009, 8, 1, 16, 0, 0); 
        options.setRoutingContext(graph, entrance, stop);
        ShortestPathTree spt = aStar.getShortestPathTree(options);
        
        GraphPath path = spt.getPath(stop, false);
        assertNotNull(path);
        assertEquals(TestUtils.dateInSeconds("America/New_York", 2009, 8, 1, 16, 0, 34), path.getEndTime());
    }
}
