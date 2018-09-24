package org.opentripplanner.util;

import java.util.ArrayList;
import java.util.Iterator;

import junit.framework.TestCase;

public class TestJoinedList extends TestCase {
    @SuppressWarnings("unchecked")
    public void testJoinedList() {
        ArrayList<Integer> list1 = new ArrayList<Integer>();
        list1.add(0);
        list1.add(1);
        list1.add(2);
        ArrayList<Integer> list2 = new ArrayList<Integer>();
        list2.add(3);
        list2.add(4);
        list2.add(5);
        JoinedList<Integer> joined = new JoinedList<Integer>(list1, list2);
        assertTrue(joined.get(0) == 0);
        assertTrue(joined.get(3) == 3);
        
        Iterator<Integer> it = joined.iterator();
        for (int i = 0; i < 6; ++i) {
            assertTrue(it.hasNext());
            assertTrue(it.next() == i);
        }
        assertFalse(it.hasNext());
    }
}
