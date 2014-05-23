package org.opentripplanner.osm;

import java.util.Set;

import com.beust.jcommander.internal.Sets;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;

import org.junit.Test;

public class NodeTrackerTest {

    /**
     * Check the NodeTracker against a stock Set<Long>.
     * This is done in ranges that will include numbers significantly greater than 2^32
     */
    @Test
    public void testAgainstSet() {
        final int N = 1000;
        // Set N numbers in each of four different ranges.
        for (long lo : new long[] {1L << 6, 1L << 16, 1L << 34, 1L << 50} ) {
            // Simultaneously set up a basic Set<Long> that will serve as a reference
            // and a NodeTracker that we want to test.
            Set<Long> numbers = Sets.newHashSet();
            NodeTracker tracker = new NodeTracker();
            long hi = 0L;
            int count = 0;
            for (int i = 0; i < 1000; i++) {
                long n = i * i + lo;
                numbers.add(n);
                tracker.add(n);
                hi = n;
                count++;
            }
            assertTrue(numbers.size() == count);
            // Check that the two set implementations match for every value in the range (including unset ones).
            // Note that a Set<Long> containing 0L returns false for contains((int)0).
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
