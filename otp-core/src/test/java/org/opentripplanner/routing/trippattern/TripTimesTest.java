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

package org.opentripplanner.routing.trippattern;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.List;

import org.junit.Test;
import org.onebusaway.gtfs.model.Route;
import org.onebusaway.gtfs.model.StopTime;
import org.onebusaway.gtfs.model.Trip;
import org.opentripplanner.gtfs.BikeAccess;
import org.opentripplanner.routing.core.RoutingRequest;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.graph.SimpleConcreteVertex;
import org.opentripplanner.routing.graph.Vertex;

public class TripTimesTest {

    private Graph _graph = new Graph();

    @Test
    public void testBikesAllowed() {
        Trip trip = new Trip();
        Route route = new Route();
        trip.setRoute(route);
        List<StopTime> stopTimes = Arrays.asList(new StopTime(), new StopTime());
        ScheduledTripTimes s = new ScheduledTripTimes(trip, stopTimes);

        RoutingRequest request = new RoutingRequest();
        Vertex v = new SimpleConcreteVertex(_graph, "", 0.0, 0.0);
        request.setRoutingContext(_graph, v, v);
        State s0 = new State(request);

        assertFalse(s.tripAcceptable(s0, null, null, true /* bicycle */, 0, true));

        BikeAccess.setForTrip(trip, BikeAccess.ALLOWED);
        assertTrue(s.tripAcceptable(s0, null, null, true /* bicycle */, 0, true));

        BikeAccess.setForTrip(trip, BikeAccess.NOT_ALLOWED);
        assertFalse(s.tripAcceptable(s0, null, null, true /* bicycle */, 0, true));
    }
}
