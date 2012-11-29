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

package org.opentripplanner.api.thrift.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.opentripplanner.api.thrift.definition.LatLng;
import org.opentripplanner.api.thrift.definition.Location;
import org.opentripplanner.api.thrift.definition.TravelMode;
import org.opentripplanner.api.thrift.definition.TripParameters;
import org.opentripplanner.routing.core.RoutingRequest;
import org.opentripplanner.routing.core.TraverseMode;
import org.opentripplanner.routing.core.TraverseModeSet;

/**
 * Tests for TripUtil class.
 * 
 * @author flamholz
 */
public class RoutingRequestBuilderTest {

	/**
	 * Test behavior for a simple trip.
	 */
	@Test
	public void testAddTripParameters() {
		TripParameters tp = new TripParameters();
		tp.addToAllowed_modes(TravelMode.WALK);
		tp.addToAllowed_modes(TravelMode.CAR);

		LatLng originLatLng = new LatLng(1.0, 2.5);
		Location origin = new Location();
		origin.setLat_lng(originLatLng);

		LatLng destLatLng = new LatLng(-3.0, 9.7);
		Location dest = new Location();
		dest.setLat_lng(destLatLng);

		tp.setOrigin(origin);
		tp.setDestination(dest);

		RoutingRequest rr = (new RoutingRequestBuilder()).addTripParameters(tp)
				.build();

		assertEquals("1.0000000,2.5000000", rr.getFrom());
		assertEquals("-3.0000000,9.7000000", rr.getTo());

		for (TravelMode tm : tp.getAllowed_modes()) {
			TraverseModeSet modeSet = rr.getModes();
			TraverseMode traverseMode = (new TravelModeWrapper(tm))
					.toTraverseMode();
			assertTrue(modeSet.contains(traverseMode));
		}
	}
	
	@Test
	public void testAddTripParametersWithStartTime() {
		TripParameters tp = new TripParameters();
		tp.setStart_time(getTimeSeconds());

		LatLng originLatLng = new LatLng(1.0, 2.5);
		Location origin = new Location();
		origin.setLat_lng(originLatLng);

		LatLng destLatLng = new LatLng(-3.0, 9.7);
		Location dest = new Location();
		dest.setLat_lng(destLatLng);

		tp.setOrigin(origin);
		tp.setDestination(dest);

		RoutingRequest rr = (new RoutingRequestBuilder()).addTripParameters(tp)
				.build();

		assertEquals("1.0000000,2.5000000", rr.getFrom());
		assertEquals("-3.0000000,9.7000000", rr.getTo());
		assertEquals(tp.getStart_time(), rr.dateTime);
		assertFalse(rr.arriveBy);
	}
	
	@Test
	public void testAddTripParametersWithArriveBy() {
		TripParameters tp = new TripParameters();
		tp.setArrive_by(getTimeSeconds() + 60*30);

		LatLng originLatLng = new LatLng(1.0, 2.5);
		Location origin = new Location();
		origin.setLat_lng(originLatLng);

		LatLng destLatLng = new LatLng(-3.0, 9.7);
		Location dest = new Location();
		dest.setLat_lng(destLatLng);

		tp.setOrigin(origin);
		tp.setDestination(dest);

		RoutingRequest rr = (new RoutingRequestBuilder()).addTripParameters(tp)
				.build();

		assertEquals("1.0000000,2.5000000", rr.getFrom());
		assertEquals("-3.0000000,9.7000000", rr.getTo());
		assertEquals(tp.getArrive_by(), rr.dateTime);
		assertTrue(rr.arriveBy);
	}

	@Test
	public void testAddTripParametersWithBothTimes() {
		TripParameters tp = new TripParameters();
		long now = getTimeSeconds();
		tp.setStart_time(now);
		tp.setArrive_by(now + 60*30);

		LatLng originLatLng = new LatLng(1.0, 2.5);
		Location origin = new Location();
		origin.setLat_lng(originLatLng);

		LatLng destLatLng = new LatLng(-3.0, 9.7);
		Location dest = new Location();
		dest.setLat_lng(destLatLng);

		tp.setOrigin(origin);
		tp.setDestination(dest);

		RoutingRequest rr = (new RoutingRequestBuilder()).addTripParameters(tp)
				.build();

		assertEquals("1.0000000,2.5000000", rr.getFrom());
		assertEquals("-3.0000000,9.7000000", rr.getTo());
		
		// Start time takes precedence
		assertEquals(tp.getStart_time(), rr.dateTime);
		assertFalse(rr.arriveBy);
	}
	
	@Test
	public void testSetNumItineraries() {
		int n = 3;
		RoutingRequest rr = (new RoutingRequestBuilder()).setNumItineraries(n)
				.build();
		assertEquals(n, rr.getNumItineraries().intValue());
	}

	@Test
	public void testSetOriginDestination() {
		LatLng origin = new LatLng(1.0, 2.5);
		LatLng dest = new LatLng(-3.0, 9.7);

		RoutingRequest rr = (new RoutingRequestBuilder()).setOrigin(origin)
				.setDestination(dest).build();
		assertEquals("1.0000000,2.5000000", rr.getFrom());
		assertEquals("-3.0000000,9.7000000", rr.getTo());
	}

	/**
	 * @return Current time in seconds.
	 */
	private long getTimeSeconds() {
		return System.currentTimeMillis() / 1000;
	}
}
