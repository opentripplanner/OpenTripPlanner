package org.opentripplanner.profile;

import junit.framework.TestCase;
import org.junit.Test;

import java.util.BitSet;

/**
 * @author abyrd
 * @created 2014-09-16
 */
public class TimeWindowTest extends TestCase {

    @Test
    public void testIntervals() {
        BitSet services = new BitSet();
        services.set(2);
        services.set(4);
        services.set(6);
        TimeWindow tw = new TimeWindow(1000, 3000, services);
        assertEquals("Service running, overlap should exist.", 500, tw.overlap(500, 1500, 2));
        assertEquals("No service codes in common, overlap should be zero.", 0, tw.overlap(500, 1500, 8));
        assertEquals("Interval entirely within window.", 1000, tw.overlap(1500, 2500, 4));
        assertEquals("Service running, but interval entirely outside window.", 0, tw.overlap(3500, 4000, 4));
    }

}
