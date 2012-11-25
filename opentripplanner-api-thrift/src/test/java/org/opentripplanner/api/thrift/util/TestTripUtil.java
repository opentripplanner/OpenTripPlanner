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

import java.util.HashMap;
import java.util.Map;

import junit.framework.TestCase;

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
public class TestTripUtil extends TestCase {

	public void testLatLngToString() {
		LatLng ll = new LatLng(1.0, 2.5);
		assertEquals("1.0,2.5", TripUtil.latLngToString(ll));

		ll = new LatLng(-3.0, 9.7);
		assertEquals("-3.0,9.7", TripUtil.latLngToString(ll));
	}

	/**
	 * Check mapping for select TraverseMode, TravelMode pairs.
	 */
	public void testGetTraverseMode() {
		Map<TravelMode, TraverseMode> m = new HashMap<TravelMode, TraverseMode>();
		m.put(TravelMode.BICYCLE, TraverseMode.BICYCLE);
		m.put(TravelMode.WALK, TraverseMode.WALK);
		m.put(TravelMode.CAR, TraverseMode.CAR);
		m.put(TravelMode.ANY_TRANSIT, TraverseMode.TRANSIT);

		for (TravelMode tm : m.keySet()) {
			TraverseMode expectedTraverse = m.get(tm);
			assertEquals(expectedTraverse, TripUtil.getTraverseMode(tm));
		}
	}

	/**
	 * Test behavior for a simple trip.
	 */
	public void testInitRoutingRequest() {
		TripParameters tp = new TripParameters();
		tp.addToAllowed_modes(TravelMode.WALK);
		tp.addToAllowed_modes(TravelMode.CAR);

		LatLng originLatLng = new LatLng(1.0, 2.0);
		Location origin = new Location(originLatLng);

		LatLng destLatLng = new LatLng(3.0, 2.0);
		Location dest = new Location(destLatLng);

		tp.setOrigin(origin);
		tp.setDestination(dest);

		RoutingRequest rr = TripUtil.initRoutingRequest(tp);

		assertEquals(TripUtil.latLngToString(originLatLng), rr.getFrom());
		assertEquals(TripUtil.latLngToString(destLatLng), rr.getTo());

		for (TravelMode tm : tp.getAllowed_modes()) {
			TraverseModeSet modeSet = rr.getModes();
			TraverseMode traverseMode = TripUtil.getTraverseMode(tm);
			assertTrue(modeSet.contains(traverseMode));
		}
	}

}
