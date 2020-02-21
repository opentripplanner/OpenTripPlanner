package org.opentripplanner.util;

import org.junit.Test;

import static org.junit.Assert.*;

public class CoordinateUtilsTest {

    @Test
    public void compare() {
        assertFalse(CoordinateUtils.compare(5.0, 60, 5.0000005, 60.0000005));
        assertTrue(CoordinateUtils.compare(5.0, 60, 5.00000005, 60.00000005));
    }
}