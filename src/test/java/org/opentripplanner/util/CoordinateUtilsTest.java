package org.opentripplanner.util;

import org.junit.Assert;
import org.junit.Test;

import static org.opentripplanner.util.CoordinateUtils.coordinateEquals;

public class CoordinateUtilsTest {

    @Test
    public void testCoordinateEquals() {
        // Test latitude
        Assert.assertTrue(coordinateEquals(5.000_000_59, 1, 5.000_000_50, 1));
        Assert.assertFalse(coordinateEquals(5.000_000_60, 1, 5.000_000_50, 1));

        // Test longitude
        Assert.assertTrue(coordinateEquals(1, 5.000_000_59, 1, 5.000_000_50));
        Assert.assertFalse(coordinateEquals(1,5.000_000_60, 1, 5.000_000_50));
    }

    @Test
    public void hash() {
        // Test that the hash uses 7 digits precision after the period (.)
        Assert.assertEquals(51234567, CoordinateUtils.hash(5.123_456_789, 0.0), 1E-9);
        Assert.assertEquals(51234567 * 31, CoordinateUtils.hash(0.0, 5.123_456_789), 1E-9);
        Assert.assertEquals(1 + 31 * 51234567, CoordinateUtils.hash(0.000_000_1, 5.123_456_789), 1E-9);
        Assert.assertEquals(-1_800_000_000, CoordinateUtils.hash(-180.0, 0.0), 1E-9);
        Assert.assertEquals(1_800_000_000, CoordinateUtils.hash(180.0, 0.0), 1E-9);
    }
}