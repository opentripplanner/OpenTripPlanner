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

import static org.junit.Assert.*;

import org.junit.Test;
import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.gtfs.model.Route;
import org.onebusaway.gtfs.model.Trip;
import org.opentripplanner.common.model.GenericLocation;

public class RoutingRequestTest {

    private GenericLocation randomLocation() {
        return new GenericLocation(Math.random(), Math.random());
    }
    
    @Test
    public void testIntermediatePlaces() {
        RoutingRequest req = new RoutingRequest();
        assertFalse(req.hasIntermediatePlaces());
        assertFalse(req.intermediatesEffectivelyOrdered());
        
        req.clearIntermediatePlaces();
        assertFalse(req.hasIntermediatePlaces());
        assertFalse(req.intermediatesEffectivelyOrdered());
        
        req.addIntermediatePlace(randomLocation());
        assertTrue(req.hasIntermediatePlaces());
        
        // There is only one intermediate, so they are effectively ordered.
        assertTrue(req.intermediatesEffectivelyOrdered());
        
        req.clearIntermediatePlaces();
        assertFalse(req.hasIntermediatePlaces());
        assertFalse(req.intermediatesEffectivelyOrdered());
        
        req.addIntermediatePlace(randomLocation());
        req.addIntermediatePlace(randomLocation());
        assertTrue(req.hasIntermediatePlaces());
        assertFalse(req.intermediatesEffectivelyOrdered());
        
        req.setIntermediatePlacesOrdered(true);
        assertTrue(req.intermediatesEffectivelyOrdered());        
    }

    @Test
    public void testPreferencesPenaltyForTrip() {
        AgencyAndId agencyAndId = new AgencyAndId();
        Route route = new Route();
        Trip trip = new Trip();
        RoutingRequest routingRequest = new RoutingRequest();

        trip.setRoute(route);
        route.setId(agencyAndId);
        assertEquals(0, routingRequest.preferencesPenaltyForTrip(trip));
    }
}
