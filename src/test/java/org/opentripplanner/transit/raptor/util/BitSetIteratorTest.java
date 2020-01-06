package org.opentripplanner.transit.raptor.util;

import org.junit.Test;

import java.util.BitSet;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class BitSetIteratorTest {

    @Test
    public void test() {
        BitSetIterator it;
        BitSet set = new BitSet(5);

        // Empty set does not have any elements
        it = new BitSetIterator(set);
        assertFalse(it.hasNext());

        set.set(2);
        it = new BitSetIterator(set);
        assertTrue(it.hasNext());
        assertEquals(2, it.next());
        assertFalse(it.hasNext());


        // set: [0, 2, 5], 2 is added above
        set.set(0);
        set.set(5);
        it = new BitSetIterator(set);
        assertTrue(it.hasNext());
        assertEquals(0, it.next());
        assertTrue(it.hasNext());
        assertEquals(2, it.next());
        assertTrue(it.hasNext());
        assertEquals(5, it.next());
        assertFalse(it.hasNext());

    }
}