package org.opentripplanner.model;


import org.junit.Assert;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class WgsCoordinateTest {
    @Test
    public void testToString() {
        WgsCoordinate c = new WgsCoordinate(1.123456789, 2.987654321);
        assertEquals("(1.12346, 2.98765)", c.toString());
        assertEquals("(1.123, 2.9)", new WgsCoordinate(1.123, 2.9).toString());
    }

    @Test
    public void testCoordinateEquals() {
        WgsCoordinate a = new WgsCoordinate(5.0, 3.0);

        // Test latitude
        WgsCoordinate sameLatitude = new WgsCoordinate(5.000_000_099, 3.0);
        WgsCoordinate differentLatitude = new WgsCoordinate(5.000_000_101, 3.0);

        Assert.assertTrue(a.sameLocation(sameLatitude));
        Assert.assertFalse(a.sameLocation(differentLatitude));

        // Test longitude
        WgsCoordinate sameLongitude = new WgsCoordinate(5.0, 3.000_000_099);
        WgsCoordinate differentLongitude = new WgsCoordinate(5.0, 3.000_000_101);

        Assert.assertTrue(a.sameLocation(sameLongitude));
        Assert.assertFalse(a.sameLocation(differentLongitude));
    }
}