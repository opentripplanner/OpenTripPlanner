package org.opentripplanner.osm;

import java.util.Set;

import junit.framework.TestCase;

import com.beust.jcommander.internal.Sets;

public class NodeTrackerTest extends TestCase {

    /** Check the NodeTracker against a stock Set<Long>. */
    public static void testAgainstSet() {
        // track big numbers > 2^32
        for (long lo : new long[] {1L << 6, 1L << 16, 1L << 34, 1L << 50} ) {
            Set<Long> numbers = Sets.newHashSet();
            NodeTracker tracker = new NodeTracker();
            long hi = 0L;
            int count = 0;
            for (int i = 0; i < 3000; i++) {            
                long n = i * i + lo;
                numbers.add(n);
                tracker.add(n);
                hi = n;
                count++;
            }
            assertTrue(numbers.size() == count);
            System.out.printf("Testing node tracker on %d numbers in range [%d,%d]\n", count, lo, hi);
            /* Note that a Set<Long> containing 0L returns false for contains((int)0). */
            for (long i = lo; i < hi; i++) {
                if (tracker.contains(i)) {
                    assertTrue(numbers.contains(i));
                } else {
                    assertFalse(numbers.contains(i));
                }
                if (numbers.contains(i)) {
                    assertTrue(tracker.contains(i));
                } else {
                    assertFalse(tracker.contains(i));
                }
            }
        }
    }
}
