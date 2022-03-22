package org.opentripplanner.transit.raptor.rangeraptor.transit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class ForwardTimeCalculatorTest {

    private final TimeCalculator subject = new ForwardTimeCalculator();

    @Test
    public void searchForward() {
        assertTrue(subject.searchForward());
    }

    @Test
    public void isBefore() {

        assertTrue(subject.isBefore(10, 11));
        assertFalse(subject.isBefore(11, 10));
        assertFalse(subject.isBefore(10, 10));
    }

    @Test
    public void isAfter() {
        assertTrue(subject.isAfter(11, 10));
        assertFalse(subject.isAfter(10, 11));
        assertFalse(subject.isAfter(10, 10));
    }

    @Test
    public void duration() {
        assertEquals(600, subject.plusDuration(500, 100));
        assertEquals(400, subject.minusDuration(500, 100));
        assertEquals(400, subject.duration(100, 500));
    }

    @Test
    public void unreachedTime() {
        assertEquals(Integer.MAX_VALUE,subject.unreachedTime());
    }
}