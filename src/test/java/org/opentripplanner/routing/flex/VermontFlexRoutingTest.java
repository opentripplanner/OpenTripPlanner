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
package org.opentripplanner.routing.flex;

import org.junit.Test;
import org.opentripplanner.ConstantsForTests;
import org.opentripplanner.api.model.BoardAlightType;
import org.opentripplanner.routing.algorithm.AStar;
import org.opentripplanner.routing.core.RoutingRequest;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.spt.GraphPath;

import java.util.List;

import static org.junit.Assert.*;

public class VermontFlexRoutingTest {

    private Graph graph = ConstantsForTests.getInstance().getVermontGraph();

    // http://otp-vtrans-qa.camsys-apps.com/local/#plan?fromPlace=44.42145960616986%2C-72.01937198638917&toPlace=44.427773287332464%2C-72.0120351442025&date=05%2F23%2F2018&time=1%3A37%20PM&mode=TRANSIT%2CWALK&numItineraries=3&wheelchairAccessible=false&flagStopBufferSize=50&useReservationServices=true&useEligibilityServices=true
    // Flag stop on both sides, on Jay-Lyn (route 1382)
    @Test
    public void testFlagStop() {
        GraphPath path = getPathToDestination("44.4214596,-72.019371", "44.4277732,-72.01203514",
                "2018-05-23", "1:37pm");
        List<Ride> rides = Ride.createRides(path);
        assertEquals(1, rides.size());
        Ride ride = rides.get(0);
        assertEquals("1382", ride.getRoute().getId());
        assertEquals(BoardAlightType.FLAG_STOP, ride.getBoardType());
        assertEquals(BoardAlightType.FLAG_STOP, ride.getBoardType());
    }

    private GraphPath getPathToDestination(String from, String to, String date, String time) {
        RoutingRequest options = new RoutingRequest();
        // defaults in vermont router-config.json
        options.maxWalkDistance = 804;
        options.maxWalkDistanceHeuristic = 5000;
        options.callAndRideReluctance = 3.0;
        options.walkReluctance = 3.0;
        options.tripDiscoveryMode = true;
        options.waitAtBeginningFactor = 0;
        options.transferPenalty = 600;
        // for testing
        options.ignoreDrtAdvanceBookMin = true;
        options.setDateTime(date, time, graph.getTimeZone());
        options.setFromString(from);
        options.setToString(to);
        options.setRoutingContext(graph);

        // Simulate GraphPathFinder - run modifiers for graph based on request
        FlagStopGraphModifier svc1 = new FlagStopGraphModifier(graph);
        DeviatedRouteGraphModifier svc2 = new DeviatedRouteGraphModifier(graph);
        svc1.createForwardHops(options);
        svc2.createForwardHops(options);
        svc1.createBackwardHops(options);
        svc2.createBackwardHops(options);

        AStar astar = new AStar();
        astar.getShortestPathTree(options);
        return astar.getPathsToTarget().iterator().next();
    }
}
