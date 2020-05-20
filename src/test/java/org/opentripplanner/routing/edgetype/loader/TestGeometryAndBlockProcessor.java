package org.opentripplanner.routing.edgetype.loader;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import junit.framework.TestCase;
import org.junit.Ignore;
import org.opentripplanner.ConstantsForTests;
import org.opentripplanner.common.geometry.GeometryUtils;
import org.opentripplanner.graph_builder.DataImportIssue;
import org.opentripplanner.graph_builder.DataImportIssueStore;
import org.opentripplanner.graph_builder.issues.NegativeHopTime;
import org.opentripplanner.graph_builder.module.StreetLinkerModule;
import org.opentripplanner.graph_builder.module.geometry.GeometryAndBlockProcessor;
import org.opentripplanner.gtfs.GtfsContext;
import org.opentripplanner.model.calendar.CalendarServiceData;
import org.opentripplanner.routing.algorithm.astar.AStar;
import org.opentripplanner.routing.core.OptimizeType;
import org.opentripplanner.routing.core.TraverseMode;
import org.opentripplanner.routing.api.request.RoutingRequest;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.core.TraverseModeSet;
import org.opentripplanner.routing.edgetype.StreetEdge;
import org.opentripplanner.routing.edgetype.StreetTransitLink;
import org.opentripplanner.routing.edgetype.StreetTraversalPermission;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.routing.spt.GraphPath;
import org.opentripplanner.routing.spt.ShortestPathTree;
import org.opentripplanner.routing.vertextype.IntersectionVertex;
import org.opentripplanner.routing.vertextype.TransitStopVertex;
import org.opentripplanner.util.TestUtils;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.TimeZone;

import static org.opentripplanner.gtfs.GtfsContextBuilder.contextBuilder;

/**
 * TODO OTP2 - Test is too close to the implementation and will need to be reimplemented.
 */
@Ignore
public class TestGeometryAndBlockProcessor extends TestCase {

    private Graph graph;
    private AStar aStar = new AStar();
    private GtfsContext context;
    private String feedId;
    private DataImportIssueStore issueStore;

    public void setUp() throws Exception {

        graph = new Graph();

        this.issueStore = new DataImportIssueStore(true);

        context = contextBuilder(ConstantsForTests.FAKE_GTFS)
                .withIssueStoreAndDeduplicator(graph)
                .build();

        feedId = context.getFeedId().getId();
        GeometryAndBlockProcessor factory = new GeometryAndBlockProcessor(context);
        factory.run(graph, issueStore);
        graph.putService(
                CalendarServiceData.class, context.getCalendarServiceData()
        );
        
        String[] stops = {
                feedId + ":A",
                feedId + ":B",
                feedId + ":C",
                feedId + ":D",
                feedId + ":E",
                feedId + ":entrance_a",
                feedId + ":entrance_b"
        };
        for (int i = 0; i < stops.length; ++i) {
            TransitStopVertex stop = (TransitStopVertex) (graph.getVertex(stops[i]));
            
            IntersectionVertex front = new IntersectionVertex(graph, "near_1_" + stop.getStop().getId(), stop.getX() + 0.0001, stop.getY() + 0.0001);
            IntersectionVertex back =  new IntersectionVertex(graph, "near_2_" + stop.getStop().getId(), stop.getX() - 0.0001, stop.getY() - 0.0001);
            
            StreetEdge street1 = new StreetEdge(front, back, GeometryUtils.makeLineString(stop.getX() + 0.0001, stop.getY() + 0.0001, stop.getX() - 0.0001, stop.getY() - 0.0001), "street", 100, StreetTraversalPermission.ALL, false);
            StreetEdge street2 = new StreetEdge(back, front, GeometryUtils.makeLineString(stop.getX() - 0.0001, stop.getY() - 0.0001, stop.getX() + 0.0001, stop.getY() + 0.0001), "street", 100, StreetTraversalPermission.ALL, true);
        }

        StreetLinkerModule ttsnm = new StreetLinkerModule();
        //Linkers aren't run otherwise
        graph.hasStreets = true;
        graph.hasTransit = true;
        ttsnm.buildGraph(graph, new HashMap<Class<?>, Object>());
    }

    public void testIssue() {
        boolean found = false;
        for (DataImportIssue it : issueStore.getIssues()) {
            if (it instanceof NegativeHopTime) {
                NegativeHopTime nht = (NegativeHopTime) it;
                assertTrue(nht.st0.getDepartureTime() > nht.st1.getArrivalTime());
                found = true;
            }
        }
        assertTrue(found);
    }

    private List<TransitStopVertex> extractStopVertices(GraphPath path) {
        List<TransitStopVertex> ret = Lists.newArrayList();
        for (State state : path.states) {
            if (state.getVertex() instanceof TransitStopVertex) {
                ret.add(((TransitStopVertex)state.getVertex()));
            }
        }
        return ret;
    }

    public void testRouting() throws Exception {

        Vertex stop_a = graph.getVertex(feedId + ":A");
        Vertex stop_b = graph.getVertex(feedId + ":B");
        Vertex stop_c = graph.getVertex(feedId + ":C");
        Vertex stop_d = graph.getVertex(feedId + ":D");
        Vertex stop_e = graph.getVertex(feedId + ":E");

        RoutingRequest options = new RoutingRequest();
        // test feed is designed for instantaneous transfers
        options.transferSlack = 0;

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
        List<TransitStopVertex> stops = extractStopVertices(path);
        assertEquals(stops.size(), 3);
        assertEquals(stops.get(1), stop_c);
        long endTime = startTime + 40 * 60;
        assertEquals(endTime, path.getEndTime());

        //A to E (change at C)
        options.setRoutingContext(graph, stop_a, stop_e);
        spt = aStar.getShortestPathTree(options);
        path = spt.getPath(stop_e, false);
        assertNotNull(path);
        stops = extractStopVertices(path);
        assertEquals(stops.size(), 3);
        assertEquals(stops.get(1), stop_c);
        endTime = startTime + 70 * 60;
        assertEquals(endTime, path.getEndTime());
    }

    public void testRoutingOverMidnight() throws Exception {
        // this route only runs on weekdays
        Vertex stop_g = graph.getVertex(feedId + ":G_depart");
        Vertex stop_h = graph.getVertex(feedId + ":H_arrive");

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

    public void testPickupDropoff() throws Exception {
        Vertex stop_o = graph.getVertex(feedId + ":O_depart");
        Vertex stop_p = graph.getVertex(feedId + ":P");
        assertEquals(2, stop_o.getOutgoing().size());

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

    public void testTraverseMode() throws Exception {
        Vertex stop_a = graph.getVertex(feedId + ":A_depart");
        Vertex stop_b = graph.getVertex(feedId + ":B_arrive");

        ShortestPathTree spt;

        RoutingRequest options = new RoutingRequest();
        options.setStreetSubRequestModes(new TraverseModeSet(TraverseMode.TRAM,TraverseMode.RAIL,TraverseMode.SUBWAY,TraverseMode.FUNICULAR,TraverseMode.GONDOLA));
        options.dateTime = TestUtils.dateInSeconds("America/New_York", 2009, 8, 0, 0, 0, 0);
        options.setRoutingContext(graph, stop_a, stop_b);
        spt = aStar.getShortestPathTree(options );

        //a to b is bus only
        assertNull(spt.getPath(stop_b, false));
        
        options.setStreetSubRequestModes(new TraverseModeSet(TraverseMode.TRAM,TraverseMode.RAIL,TraverseMode.SUBWAY,TraverseMode.FUNICULAR,TraverseMode.GONDOLA,TraverseMode.CABLE_CAR,TraverseMode.BUS));
        spt = aStar.getShortestPathTree(options);

        assertNotNull(spt.getPath(stop_b, false));
    }
    
    public void testTimelessStops() throws Exception {
        Vertex stop_d = graph.getVertex(feedId + ":D");
        Vertex stop_c = graph.getVertex(feedId + ":C");
        RoutingRequest options = new RoutingRequest();
        options.dateTime = TestUtils.dateInSeconds("America/New_York", 2009, 8, 1, 10, 0, 0);
        options.setRoutingContext(graph, stop_d, stop_c);
        ShortestPathTree spt = aStar.getShortestPathTree(options);

        GraphPath path = spt.getPath(stop_c, false);
        assertNotNull(path);
        assertEquals(TestUtils.dateInSeconds("America/New_York", 2009, 8, 1, 11, 0, 0), path.getEndTime());
    }

    public void testTripBikesAllowed() throws Exception {
        Vertex stop_a = graph.getVertex(feedId + ":A");
        Vertex stop_b = graph.getVertex(feedId + ":B");
        Vertex stop_c = graph.getVertex(feedId + ":C");
        Vertex stop_d = graph.getVertex(feedId + ":D");

        RoutingRequest options = new RoutingRequest();
        options.streetSubRequestModes.setWalk(false);
        options.streetSubRequestModes.setBicycle(true);
        options.streetSubRequestModes.setTransit(true);
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
        Vertex near_a = graph.getVertex("near_1_" + feedId + "_entrance_a");
        Vertex near_b = graph.getVertex("near_1_" + feedId + "_entrance_b");
        Vertex near_c = graph.getVertex("near_1_" + feedId + "_C");
        Vertex near_e = graph.getVertex("near_1_" + feedId + "_E");

        Vertex stop_d = graph.getVertex(feedId + ":D");
        Vertex split_d = null;
        for (StreetTransitLink e : Iterables.filter(stop_d.getOutgoing(), StreetTransitLink.class)) {
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
        
        // stop E has no accessibility information, but we should still be able to route to it.
        options.setRoutingContext(graph, near_a, near_e);
        spt = aStar.getShortestPathTree(options);

        path = spt.getPath(near_e, false);
        assertNotNull(path);

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
        Vertex destination = graph.getVertex(feedId + ":T");
        RoutingRequest options = new RoutingRequest();
        // test is designed such that transfers must be instantaneous
        options.transferSlack = 0;
        GregorianCalendar startTime = new GregorianCalendar(2009, 11, 2, 8, 30, 0);
        startTime.setTimeZone(TimeZone.getTimeZone("America/New_York"));
        options.dateTime = TestUtils.toSeconds(startTime);
        options.setRoutingContext(graph, feedId + ":Q", destination.getLabel());
        ShortestPathTree spt = aStar.getShortestPathTree(options);
        GraphPath path = spt.getPath(destination, false);

        long endTime = path.getEndTime();
        Calendar c = new GregorianCalendar();
        c.setTimeInMillis(endTime * 1000L);
        assertTrue(endTime - TestUtils.toSeconds(startTime) < 7200);
    }

    public void testFrequencies() {
        Vertex stop_u = graph.getVertex(feedId + ":U_depart");
        Vertex stop_v = graph.getVertex(feedId + ":V_arrive");

        ShortestPathTree spt;
        GraphPath path;

        RoutingRequest options = new RoutingRequest();
        options.setStreetSubRequestModes(new TraverseModeSet(TraverseMode.TRANSIT));
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
        
        // TODO more detailed testing of frequencies

    }
    
    public void testFewestTransfers() {
        Vertex stop_c = graph.getVertex(feedId + ":C");
        Vertex stop_d = graph.getVertex(feedId + ":D");
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
        options.transferCost = 1800;
        spt = aStar.getShortestPathTree(options);

        path = spt.getPath(stop_d, false);
        assertNotNull(path);
        assertEquals(TestUtils.dateInSeconds("America/New_York", 2009, 8, 1, 16, 50, 0), path.getEndTime());

    }

    public void testPathways() throws Exception {

        Vertex entrance = graph.getVertex(feedId + ":entrance_a");
        assertNotNull(entrance);
        Vertex stop = graph.getVertex(feedId + ":A");
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
