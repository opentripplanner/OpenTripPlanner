package org.opentripplanner.routing.edgetype;

import java.io.IOException;

import junit.framework.TestCase;

import org.opentripplanner.ConstantsForTests;
import org.opentripplanner.common.IterableLibrary;
import org.opentripplanner.routing.core.RoutingRequest;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.routing.trippattern.TripTimes;
import org.opentripplanner.routing.vertextype.TransitStopDepart;
import org.opentripplanner.util.TestUtils;

public class TestTripPatterns extends TestCase {
    
    public void testDepartureSearch () throws IOException {

        Graph graph = ConstantsForTests.buildGraph(ConstantsForTests.GENERATED_GTFS);
        RoutingRequest options = new RoutingRequest();
        
        int h = 13, m = 0, s = 0;
        long time = TestUtils.dateInSeconds("Africa/Accra", 2009, 8, 7, h, m, s);
        options.dateTime = time;
        Vertex ov = graph.getVertex("TEST_A0");
        Vertex dv = graph.getVertex("TEST_B5");
        options.setRoutingContext(graph, ov, dv);
        
        //ServiceDay sd = new ServiceDay();
        int secSinceMidnight = s + m*60 + h*60*60;
        TransitStopDepart odv = (TransitStopDepart) IterableLibrary.filter(
                ov.getOutgoing(), PreBoardEdge.class).iterator().next().getToVertex();
        
        TripTimes[] ttArray = new TripTimes[4]; 
        for (TransitBoardAlight tba : IterableLibrary.filter(odv.getOutgoing(), TransitBoardAlight.class)) {
            System.out.println();
            tba.getPattern().finish();
            TripTimes tt = tba.getPattern().getNextTrip(2, secSinceMidnight, false, options, true, ttArray);
            System.out.println("main response : " + tt);
            System.out.println("adjacent : " + ttArray);
            for (int i=0; i < ttArray.length; i++)
                System.out.println(i + "=" + ttArray[i]);
                
        }
        assertTrue(time > 0);
        
    }

}
