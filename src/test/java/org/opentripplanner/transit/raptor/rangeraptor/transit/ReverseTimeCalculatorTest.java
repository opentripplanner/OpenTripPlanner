package org.opentripplanner.transit.raptor.rangeraptor.transit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class ReverseTimeCalculatorTest {

    private final TimeCalculator subject = new ReverseTimeCalculator();

    @Test
    public void searchForward() {
        assertFalse(subject.searchForward());
    }

    @Test
    public void isBefore() {
        assertTrue(subject.isBefore(11, 10));
        assertFalse(subject.isBefore(10, 11));
        assertFalse(subject.isBefore(10, 10));
    }

    @Test
    public void isAfter() {
        assertTrue(subject.isAfter(10, 11));
        assertFalse(subject.isAfter(11, 10));
        assertFalse(subject.isAfter(10, 10));
    }

    @Test
    public void duration() {
        assertEquals(400, subject.plusDuration(500, 100));
        assertEquals(600, subject.minusDuration(500, 100));
        assertEquals(400, subject.duration(500, 100));
    }

    @Test
    public void unreachedTime() {
        assertEquals(Integer.MIN_VALUE, subject.unreachedTime());
    }
}