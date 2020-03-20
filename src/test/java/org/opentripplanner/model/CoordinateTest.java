package org.opentripplanner.model;


import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

public class CoordinateTest {
    private final Coordinate c = new Coordinate(1.123456789, 2.987654321);

    // The sameAs differ only in the lower precession digits
    private final Coordinate sameAs = new Coordinate(1.1234567891111, 2.987654321111);

    // Differ in digits number 6 after the period (.)
    private final Coordinate other = new Coordinate(1.123457, 2.987654300);

    @Test
    public void testEquals() {
        assertEquals(c, sameAs);
        assertNotEquals(c, other);
    }

    @Test
    public void testHashCode() {
        assertEquals(c.hashCode(), sameAs.hashCode());
        assertNotEquals(c.hashCode(), other.hashCode());
    }

    @Test
    public void testToString() {
        assertEquals("(1.12346, 2.98765)", c.toString());
        assertEquals("(1.123, 2.9)", new Coordinate(1.123, 2.9).toString());
    }
}