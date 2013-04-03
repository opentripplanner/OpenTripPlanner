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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import junit.framework.TestCase;

import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.gtfs.model.Route;
import org.opentripplanner.ConstantsForTests;
import org.opentripplanner.gtfs.GtfsLibrary;
import org.opentripplanner.routing.core.RouteSpec;
import org.opentripplanner.routing.core.RoutingRequest;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.edgetype.PatternHop;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.routing.request.BannedStopSet;
import org.opentripplanner.routing.spt.GraphPath;
import org.opentripplanner.routing.spt.ShortestPathTree;
import org.opentripplanner.util.TestUtils;

public class TestBanning extends TestCase {

    GenericAStar aStar = new GenericAStar();

    public void testBannedRoutes() {

        Graph graph = ConstantsForTests.getInstance().getPortlandGraph();

        RoutingRequest options = new RoutingRequest();
        Vertex start = graph.getVertex("TriMet_8371");
        Vertex end = graph.getVertex("TriMet_8374");
        options.dateTime = TestUtils.dateInSeconds("America/Los_Angeles", 2009, 11, 1, 12, 34, 25);
        // must set routing context _after_ options is fully configured (time)
        options.setRoutingContext(graph, start, end);
        ShortestPathTree spt = null;

        /*
         * The MAX Red, Blue, and Green lines all run along the same trackage between the stops 8374 and 8371. Together, they form the white line. No,
         * wait, that's light. They make a pretty good test case for banned routes, since if one is banned, you can always take another.
         */
        String[][] maxLines = { { "MAX Red Line", null }, { "MAX Blue Line", null },
                { "MAX Green Line", null }, { null, "90" }, { null, "100" }, { null, "200" } };
        for (int i = 0; i < maxLines.length; ++i) {
            String lineName = maxLines[i][0];
            String lineId = maxLines[i][1];
            String routeSpecStr = "TriMet_" + (lineName != null ? lineName : "")
                    + (lineId != null ? "_" + lineId : "");
            RouteSpec bannedRouteSpec = new RouteSpec(routeSpecStr);
            options.bannedRoutes.add(bannedRouteSpec);
            spt = aStar.getShortestPathTree(options);
            GraphPath path = spt.getPath(end, true);
            for (State s : path.states) {
                if (s.getBackEdge() instanceof PatternHop) {
                    PatternHop e = (PatternHop) s.getBackEdge();
                    Route route = e.getPattern().getExemplar().getRoute();
                    RouteSpec routeSpec = new RouteSpec(route.getId().getAgencyId(),
                            GtfsLibrary.getRouteName(route), route.getId().getId());
                    assertFalse(routeSpec.equals(bannedRouteSpec));
                    boolean foundMaxLine = false;
                    for (int j = 0; j < maxLines.length; ++j) {
                        if (j != i) {
                            if (e.getName().equals(maxLines[j][0])) {
                                foundMaxLine = true;
                            }
                        }
                    }
                    assertTrue(foundMaxLine);
                }
            }
            options.bannedRoutes.clear();
        }
    }

    /**
     * Test trip banning. We compute a set of shortest routes between two random stops in the Portland graph. We then ban, for each route, up to a
     * certain amount of trips used in this route, one by one, and recompute the path. The banned trips must not appear in the new computed route.
     */
    public void testBannedTrips() {

        Graph graph = ConstantsForTests.getInstance().getPortlandGraph();
        Random rand = new Random(42); // Make test reproducible... Do not change this constant

        for (int i = 0; i < 20; i++) {
            RoutingRequest options = new RoutingRequest();
            options.dateTime = TestUtils.dateInSeconds("America/Los_Angeles", 2009, 11, 1, 12, 34,
                    25);
            // Pick two random locations
            Vertex start = null;
            Vertex end = null;
            while (start == null)
                start = graph.getVertex("TriMet_" + rand.nextInt(10000));
            while (end == null)
                end = graph.getVertex("TriMet_" + rand.nextInt(10000));
            options.setRoutingContext(graph, start, end);
            ShortestPathTree spt = null;

            int n = rand.nextInt(5) + 3;
            for (int j = 0; j < n; j++) {
                spt = aStar.getShortestPathTree(options);
                GraphPath path = spt.getPath(end, true);
                if (path == null || spt == null)
                    break; // No path found
                Set<AgencyAndId> usedTripIds = new HashSet<AgencyAndId>();
                for (State s : path.states) {
                    AgencyAndId tripId = s.getTripId();
                    // Ban the trip for next round
                    if (tripId != null)
                        usedTripIds.add(tripId);
                }
                // Used trips should not contains a banned trip
                for (AgencyAndId usedTripId : usedTripIds)
                    assertFalse(options.bannedTrips.containsKey(usedTripId));
                if (usedTripIds.size() == 0)
                    break; // Not a transit trip, no sense to ban trip any longer
                // Pick a random used trip to ban
                List<AgencyAndId> usedTripIdsList = new ArrayList<AgencyAndId>(usedTripIds);
                AgencyAndId tripToBan = usedTripIdsList.get(rand.nextInt(usedTripIds.size()));
                options.banTrip(tripToBan);
            }
            options.bannedTrips.clear();
        }
    }
}
