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

package org.opentripplanner.gtfs;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.onebusaway.gtfs.model.Route;
import org.onebusaway.gtfs.model.Trip;

public class BikeAccessTest {

    @Test
    public void testBikesAllowed() {
        Trip trip = new Trip();
        Route route = new Route();
        trip.setRoute(route);

        assertEquals(BikeAccess.UNKNOWN, BikeAccess.fromTrip(trip));
        trip.setBikesAllowed(1);
        assertEquals(BikeAccess.ALLOWED, BikeAccess.fromTrip(trip));
        trip.setBikesAllowed(2);
        assertEquals(BikeAccess.NOT_ALLOWED, BikeAccess.fromTrip(trip));
        route.setBikesAllowed(1);
        assertEquals(BikeAccess.NOT_ALLOWED, BikeAccess.fromTrip(trip));
        trip.setBikesAllowed(0);
        assertEquals(BikeAccess.ALLOWED, BikeAccess.fromTrip(trip));
        route.setBikesAllowed(2);
        assertEquals(BikeAccess.NOT_ALLOWED, BikeAccess.fromTrip(trip));
    }

    @SuppressWarnings("deprecation")
    @Test
    public void testTripBikesAllowed() {
        Trip trip = new Trip();
        Route route = new Route();
        trip.setRoute(route);

        assertEquals(BikeAccess.UNKNOWN, BikeAccess.fromTrip(trip));
        trip.setTripBikesAllowed(2);
        assertEquals(BikeAccess.ALLOWED, BikeAccess.fromTrip(trip));
        trip.setTripBikesAllowed(1);
        assertEquals(BikeAccess.NOT_ALLOWED, BikeAccess.fromTrip(trip));
        route.setRouteBikesAllowed(2);
        assertEquals(BikeAccess.NOT_ALLOWED, BikeAccess.fromTrip(trip));
        trip.setTripBikesAllowed(0);
        assertEquals(BikeAccess.ALLOWED, BikeAccess.fromTrip(trip));
        route.setRouteBikesAllowed(1);
        assertEquals(BikeAccess.NOT_ALLOWED, BikeAccess.fromTrip(trip));
    }

    @SuppressWarnings("deprecation")
    @Test
    public void testBikesAllowedOverridesTripBikesAllowed() {
        Trip trip = new Trip();
        Route route = new Route();
        trip.setRoute(route);

        trip.setBikesAllowed(1);
        trip.setTripBikesAllowed(1);
        assertEquals(BikeAccess.ALLOWED, BikeAccess.fromTrip(trip));
        trip.setBikesAllowed(2);
        trip.setTripBikesAllowed(2);
        assertEquals(BikeAccess.NOT_ALLOWED, BikeAccess.fromTrip(trip));
    }

    @SuppressWarnings("deprecation")
    @Test
    public void setBikesAllowed() {
        Trip trip = new Trip();
        BikeAccess.setForTrip(trip, BikeAccess.ALLOWED);
        assertEquals(1, trip.getBikesAllowed());
        assertEquals(2, trip.getTripBikesAllowed());
        BikeAccess.setForTrip(trip, BikeAccess.NOT_ALLOWED);
        assertEquals(2, trip.getBikesAllowed());
        assertEquals(1, trip.getTripBikesAllowed());
        BikeAccess.setForTrip(trip, BikeAccess.UNKNOWN);
        assertEquals(0, trip.getBikesAllowed());
        assertEquals(0, trip.getTripBikesAllowed());
    }
}
