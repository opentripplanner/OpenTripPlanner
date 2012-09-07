package org.opentripplanner.graph_builder.impl.raptor;

import static org.junit.Assert.*;

import java.util.Iterator;

import org.junit.Test;

public class ArrayBagTest {

    @Test
    public void testArrayBag() {
        ArrayBag<Integer> bag = new ArrayBag<Integer>();
        
        //a one-item bag
        bag.add(1);
        assertEquals(1, bag.size());
        Iterator<Integer> it = bag.iterator();
        assertTrue(it.hasNext());
        int j = it.next();
        assertEquals(1, j);
        assertFalse(it.hasNext());
        it.remove();
        assertEquals(0, bag.size());
        assertFalse(it.hasNext());
        
        // a two-item bag
        bag.add(1);
        bag.add(2);
        assertEquals(2, bag.size());

        it = bag.iterator();
        assertTrue(it.hasNext());
        j = it.next();
        assertEquals(1, j);
        assertTrue(it.hasNext());
        //test removal; this should not break iteration
        it.remove();
        assertEquals(1, bag.size());
        assertTrue(it.hasNext());
        j = it.next();
        assertEquals(2, j);
        assertFalse(it.hasNext());

        bag = new ArrayBag<Integer>();
        bag.add(1);
        bag.add(2);
        bag.add(3);
        
        it = bag.iterator();
        assertTrue(it.hasNext());
        j = it.next();
        j = it.next();
        it.remove();
        j = it.next();
        assertEquals(3, j);
        it.remove();
        assertFalse(it.hasNext());

        
    }

}
