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

package org.opentripplanner.routing.algorithm;

import java.io.File;
import java.util.Random;

import junit.framework.TestCase;

import org.onebusaway.gtfs.model.calendar.CalendarServiceData;
import org.opentripplanner.ConstantsForTests;
import org.opentripplanner.gtfs.GtfsContext;
import org.opentripplanner.gtfs.GtfsLibrary;
import org.opentripplanner.routing.core.RoutingRequest;
import org.opentripplanner.routing.edgetype.factory.GTFSPatternHopFactory;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.routing.spt.GraphPath;
import org.opentripplanner.routing.spt.ShortestPathTree;
import org.opentripplanner.util.TestUtils;

public class TestAStar extends TestCase {
    
    GenericAStar aStar = new GenericAStar();

    public void testBasic() throws Exception {

        GtfsContext context = GtfsLibrary.readGtfs(new File(ConstantsForTests.CALTRAIN_GTFS));

        Graph gg = new Graph();
        GTFSPatternHopFactory factory = new GTFSPatternHopFactory(context);
        factory.run(gg);
        gg.putService(CalendarServiceData.class, GtfsLibrary.createCalendarServiceData(context.getDao()));
        RoutingRequest options = new RoutingRequest();
        
        ShortestPathTree spt;
        GraphPath path = null;

        options.dateTime = TestUtils.dateInSeconds("America/Los_Angeles", 2009, 8, 7, 12, 0, 0);
        options.setRoutingContext(gg, "Caltrain_Millbrae Caltrain", "Caltrain_Mountain View Caltrain");
        spt = aStar.getShortestPathTree(options);
        path = spt.getPath(gg.getVertex("Caltrain_Mountain View Caltrain"), true);

        long endTime = TestUtils.dateInSeconds("America/Los_Angeles", 2009, 8, 7, 13, 29, 0);

        assertEquals(path.getEndTime(), endTime);

        /* test backwards traversal */
        options.setArriveBy(true);
        options.dateTime = endTime;
        options.setRoutingContext(gg, "Caltrain_Millbrae Caltrain", "Caltrain_Mountain View Caltrain");
        spt = aStar.getShortestPathTree(options);
        path = spt.getPath(gg.getVertex("Caltrain_Millbrae Caltrain"), true);

        long expectedStartTime = TestUtils.dateInSeconds("America/Los_Angeles", 2009, 8, 7, 12, 39, 0);

        assertTrue(path.getStartTime() - expectedStartTime <= 1);

    }

    public void testMaxTime() {

        Graph graph = ConstantsForTests.getInstance().getPortlandGraph();
        Vertex start = graph.getVertex("TriMet_8371");
        Vertex end = graph.getVertex("TriMet_8374");

        RoutingRequest options = new RoutingRequest();
        long startTime = TestUtils.dateInSeconds("America/Los_Angeles", 2009, 11, 1, 12, 34, 25);
        options.dateTime = startTime;
        // one hour is more than enough time
        options.worstTime = startTime + 60 * 60; 
        options.setRoutingContext(graph, start, end);

        ShortestPathTree spt = aStar.getShortestPathTree(options);
        GraphPath path = spt.getPath(end, true);
        assertNotNull(path);
        
        // but one minute is not enough
        options.worstTime = startTime + 60; 
        spt = aStar.getShortestPathTree(options);
        path = spt.getPath(end, true);
        assertNull(path);        
    }

    public void testPerformance() throws Exception {

        Graph graph = ConstantsForTests.getInstance().getPortlandGraph();
        long startTime = TestUtils.dateInSeconds("America/Los_Angeles", 2009, 11, 1, 12, 34, 25);
        RoutingRequest options = new RoutingRequest();
        options.dateTime = startTime;
        
        Vertex airport = graph.getVertex("TriMet_10579");

        long startClock, endClock;
        Random rng = new Random();
        rng.setSeed(0);
        
        final int n_trials = 100;
        String random[] = new String[n_trials];
        for (int i = 0; i < n_trials; ++i) {
            String label;
            while (true) {
                int rand_id = rng.nextInt() % 10000;
                label = "TriMet_" + rand_id;
                if (graph.getVertex(label) != null) {
                    break;
                }
            }
            random[i] = label;
        }

        /* time A* */
        startClock = System.nanoTime();
        ShortestPathTree spt = null;
        for (int i = 0; i < n_trials; ++i) {
            options.setRoutingContext(graph, random[i], airport.getLabel());
            spt = aStar.getShortestPathTree(options);
        }

        endClock = System.nanoTime();
        long aStarTime = endClock - startClock;

        GraphPath path = spt.getPath(airport, true);
        assertNotNull("A path could not be found to the airport from " + random[n_trials - 1], path);
        double time = aStarTime / n_trials / 1000000000.0;
        assertTrue("Actual time " + time + "s greater than 500 ms", time <= 0.5);

    }
}
