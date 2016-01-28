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

package org.opentripplanner.routing.core;

import java.io.File;
import java.util.Date;
import java.util.Random;

import junit.framework.TestCase;

import org.onebusaway.gtfs.model.Stop;
import org.onebusaway.gtfs.model.calendar.CalendarServiceData;
import org.opentripplanner.ConstantsForTests;
import org.opentripplanner.gtfs.GtfsContext;
import org.opentripplanner.gtfs.GtfsLibrary;
import org.opentripplanner.routing.algorithm.AStar;
import org.opentripplanner.routing.edgetype.OnBoardDepartPatternHop;
import org.opentripplanner.routing.edgetype.PatternHop;
import org.opentripplanner.routing.edgetype.factory.GTFSPatternHopFactory;
import org.opentripplanner.routing.graph.Edge;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.routing.spt.GraphPath;
import org.opentripplanner.routing.spt.ShortestPathTree;
import org.opentripplanner.routing.trippattern.TripTimes;
import org.opentripplanner.routing.vertextype.OnboardDepartVertex;
import org.opentripplanner.routing.vertextype.PatternArriveVertex;
import org.opentripplanner.routing.vertextype.PatternStopVertex;
import org.opentripplanner.util.TestUtils;

/**
 * Test on-board routing, ie routing with a starting point "on-board" a vehicle.
 * 
 * @author laurent
 */
public class TestOnBoardRouting extends TestCase {

    private boolean verbose = false;

    private Graph graph;

    private AStar aStar = new AStar();

    public void setUp() throws Exception {
        GtfsContext context = GtfsLibrary.readGtfs(new File(ConstantsForTests.FAKE_GTFS));
        graph = new Graph();
        GTFSPatternHopFactory factory = new GTFSPatternHopFactory(context);
        factory.run(graph);
        graph.putService(CalendarServiceData.class,
                GtfsLibrary.createCalendarServiceData(context.getDao()));
    }

    /**
     * Compute a set of path between two random stop locations in a test GTFS.
     * 
     * For each departure/arrival location, compute a normal path (depart alighted). Then re-run the
     * same itinerary but with departure while on-board at a randomly-picked up trip alongside the
     * path.
     * 
     * We assert that the two itineraries will arrive at the same time, at the same place, with at
     * least one less boarding, and take a less or equals amount of time.
     */
    @SuppressWarnings("deprecation")
    public void testOnBoardRouting() throws Exception {

        String feedId = graph.getFeedIds().iterator().next();
        // Seed the random generator to make consistent set of tests
        Random rand = new Random(42);

        // Number of tests to run
        final int NTESTS = 100;

        int n = 0;
        while (true) {

            /* Compute a normal path between two random stops... */
            Vertex origin, destination;
            do {
                /* See FAKE_GTFS for available locations */
                origin = graph.getVertex(feedId + ":" + (char) (65 + rand.nextInt(20)));
                destination = graph.getVertex(feedId + ":" + (char) (65 + rand.nextInt(20)));
            } while (origin.equals(destination));

            /* ...at a random date/time */
            RoutingRequest options = new RoutingRequest();
            options.dateTime = TestUtils.dateInSeconds("America/New_York", 2009,
                    5 + rand.nextInt(4), 1 + rand.nextInt(20), 4 + rand.nextInt(10),
                    rand.nextInt(60), 0);

            ShortestPathTree spt;
            GraphPath path;

            options.setRoutingContext(graph, origin, destination);
            spt = aStar.getShortestPathTree(options);

            path = spt.getPath(destination, false);
            if (path == null)
                continue;

            System.out.println("Testing path between " + origin.getLabel() + " and "
                    + destination.getLabel() + " at " + new Date(options.dateTime * 1000));

            long arrivalTime1 = 0L;
            long elapsedTime1 = 0L;
            int numBoardings1 = 0;
            Vertex arrivalVertex1 = null;
            if (verbose)
                System.out.println("PATH 1 ---------------------");
            for (State s : path.states) {
                if (verbose)
                    System.out.println(s + " [" + s.getVertex().getClass().getName() + "]");
                arrivalTime1 = s.getTimeSeconds();
                arrivalVertex1 = s.getVertex();
                elapsedTime1 = s.getElapsedTimeSeconds();
                numBoardings1 = s.getNumBoardings();
            }

            /* Get a random transit hop from the computed path */
            Stop end = null;
            PatternStopVertex nextV = null;
            TripTimes tripTimes = null;
            int stopIndex = 0;
            long newStart = 0L;
            int nhop = 0;
            for (State s : path.states) {
                if (s.getVertex() instanceof PatternArriveVertex
                        && s.getBackEdge() instanceof PatternHop)
                    nhop++;
            }
            int hop = rand.nextInt(nhop);
            nhop = 0;
            float k = rand.nextFloat();
            for (State s : path.states) {
                Vertex v = s.getVertex();
                if (v instanceof PatternArriveVertex && s.getBackEdge() instanceof PatternHop) {
                    if (hop == nhop) {
                        PatternArriveVertex pav = (PatternArriveVertex) v;
                        end = pav.getStop();
                        nextV = pav;
                        PatternHop phe = (PatternHop) s.getBackEdge();
                        stopIndex = phe.getStopIndex();
                        tripTimes = s.getTripTimes();
                        int hopDuration = tripTimes.getRunningTime(stopIndex);
                        /*
                         * New start time at k% of hop. Note: do not try to make: round(time +
                         * k.hop) as it will be off few seconds due to floating-point rounding
                         * errors.
                         */
                        newStart = s.getBackState().getTimeSeconds() + Math.round(hopDuration * k);
                        break;
                    }
                    nhop++;
                }
            }
            System.out.println("Boarded depart: trip=" + tripTimes.trip + ", nextStop="
                    + nextV.getStop() + " stopIndex=" + stopIndex + " startTime="
                    + new Date(newStart * 1000L));

            /* And use it for onboard departure */
            double lat = end.getLat();
            double lon = end.getLon(); // Mock location, not really important here.
            OnboardDepartVertex onboardOrigin = new OnboardDepartVertex("OnBoard_Origin", lat, lon);
            @SuppressWarnings("unused")
            OnBoardDepartPatternHop currentHop = new OnBoardDepartPatternHop(onboardOrigin, nextV,
                    tripTimes, options.rctx.serviceDays.get(1), stopIndex, k);

            options.dateTime = newStart;
            options.setRoutingContext(graph, onboardOrigin, destination);
            spt = aStar.getShortestPathTree(options);

            /* Re-compute a new path starting boarded */
            GraphPath path2 = spt.getPath(destination, false);
            assertNotNull(path2);
            if (verbose)
                System.out.println("PATH 2 ---------------------");
            long arrivalTime2 = 0L;
            long elapsedTime2 = 0L;
            int numBoardings2 = 0;
            Vertex arrivalVertex2 = null;
            for (State s : path2.states) {
                if (verbose)
                    System.out.println(s + " [" + s.getVertex().getClass().getName() + "]");
                arrivalTime2 = s.getTimeSeconds();
                arrivalVertex2 = s.getVertex();
                elapsedTime2 = s.getElapsedTimeSeconds();
                numBoardings2 = s.getNumBoardings();
            }
            /* Arrival time and vertex *must* match */
            assertEquals(arrivalTime1, arrivalTime2);
            assertEquals(arrivalVertex1, destination);
            assertEquals(arrivalVertex2, destination);
            /* On-board *must* be shorter in time */
            assertTrue(elapsedTime2 <= elapsedTime1);
            /* On-board *must* have less boardings */
            assertTrue(numBoardings2 < numBoardings1);

            /* Cleanup edges */
            for (Edge edge : onboardOrigin.getOutgoing()) {
                graph.removeEdge(edge);
            }

            n++;
            if (n > NTESTS)
                break;
        }
    }
}
