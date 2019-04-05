package org.opentripplanner.routing.util;

import junit.framework.TestCase;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.CoordinateSequence;
import org.locationtech.jts.geom.impl.PackedCoordinateSequenceFactory;

public class TestElevationUtils extends TestCase {

	public void testLengthMultiplier() {

		PackedCoordinateSequenceFactory factory = PackedCoordinateSequenceFactory.DOUBLE_FACTORY;
		CoordinateSequence seq = factory.create(new Coordinate[] {
				new Coordinate(0, 1), new Coordinate(10, 1) });
		SlopeCosts costs = ElevationUtils.getSlopeCosts(seq, false);
		assertEquals(1.0, costs.lengthMultiplier);
		
		seq = factory.create(new Coordinate[] {
				new Coordinate(0, 1), new Coordinate(10, 2) });
                costs = ElevationUtils.getSlopeCosts(seq, false);
		assertEquals(1.00498756211208902702, costs.lengthMultiplier);
		
		seq = factory.create(new Coordinate[] {
				new Coordinate(0, 1), new Coordinate(10, 2), new Coordinate(15, 1) });
                costs = ElevationUtils.getSlopeCosts(seq, false);
		assertEquals(1.00992634231424500668, costs.lengthMultiplier);
	}

	public void testCalculateSlopeWalkEffectiveLengthFactor() {
		// 35% should hit the MAX_SLOPE_WALK_EFFECTIVE_LENGTH_FACTOR=3, hence 300m is expected
		assertEquals(300.0, ElevationUtils.calculateEffectiveWalkLength(100, 35), 0.1);

		// 10% incline equals 1.42 penalty
		assertEquals(141.9, ElevationUtils.calculateEffectiveWalkLength(100, 10), 0.1);

		// Flat is flat, no penalty
		assertEquals(120.0, ElevationUtils.calculateEffectiveWalkLength(120, 0));

		// 5% downhill is the fastest to walk and effective distance only 0.83 * flat distance
		assertEquals(83.9, ElevationUtils.calculateEffectiveWalkLength(100, -5), 0.1);

		// 10% downhill is about the same as flat
		assertEquals(150.0, ElevationUtils.calculateEffectiveWalkLength(150, -15));

		// 15% downhill have a penalty of 1.19
		assertEquals(238.2, ElevationUtils.calculateEffectiveWalkLength(200, -30), 0.1);

		// 45% downhill hit the MAX_SLOPE_WALK_EFFECTIVE_LENGTH_FACTOR=3 again
		assertEquals(300.0, ElevationUtils.calculateEffectiveWalkLength(100, -45), 0.1);
	}
}
