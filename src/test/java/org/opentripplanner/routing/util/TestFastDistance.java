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

package org.opentripplanner.routing.util;

import java.util.Random;

import junit.framework.TestCase;

import org.opentripplanner.common.geometry.SphericalDistanceLibrary;

public class TestFastDistance extends TestCase {

	private static final int N_TEST = 100000;
	private static final double MAX_LAT = 70.0;
	private static final double MAX_DELTA_LAT = 6.0;
	private static final double MAX_DELTA_LON = 6.0;
	
    public void testFastDistance() {

    	// Seed the random generator, if we have a failure
    	// we'd like to be able to reproduce it...
    	Random r = new Random(42);

    	for (int i = 0; i < N_TEST; i++) {
    		double lat1 = r.nextDouble() * 2.0 * MAX_LAT - MAX_LAT;
    		double lon1 = r.nextDouble() * 360.0;
    		double lat2 = lat1 + r.nextDouble() * 2.0 * MAX_DELTA_LAT - MAX_DELTA_LAT;
    		double lon2 = lon1 + r.nextDouble() * 2.0 * MAX_DELTA_LON - MAX_DELTA_LON;
    		double de = SphericalDistanceLibrary.distance(lat1, lon1, lat2, lon2);
    		double da = SphericalDistanceLibrary.fastDistance(lat1, lon1, lat2, lon2);
    		assertTrue(da <= de);
    		assertTrue(da >= de / 1.00054);
    	}
    }

}
