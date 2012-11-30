package org.opentripplanner.api.thrift.util;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.HashSet;
import java.util.Set;

import org.junit.Test;
import org.opentripplanner.api.thrift.definition.TravelMode;
import org.opentripplanner.routing.core.TraverseModeSet;

public class TravelModeSetTest {

	@Test
	public void testAdd() {
		TravelModeSet modeSet = new TravelModeSet();
		modeSet.add(TravelMode.WALK);
		modeSet.add(TravelMode.ANY_TRAIN);

		TraverseModeSet traverseModes = modeSet.toTraverseModeSet();
		assertTrue(traverseModes.getWalk());
		assertTrue(traverseModes.getTrainish());
		assertFalse(traverseModes.getBicycle());
	}

	@Test
	public void testConstructFromSet() {
		Set<TravelMode> modes = new HashSet<TravelMode>(3);

		modes.add(TravelMode.WALK);
		modes.add(TravelMode.ANY_TRAIN);

		TravelModeSet modeSet = new TravelModeSet(modes);
		TraverseModeSet traverseModes = modeSet.toTraverseModeSet();
		assertTrue(traverseModes.getWalk());
		assertTrue(traverseModes.getTrainish());
		assertFalse(traverseModes.getBicycle());
	}

}
