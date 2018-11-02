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
