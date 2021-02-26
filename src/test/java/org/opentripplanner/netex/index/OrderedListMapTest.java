package org.opentripplanner.netex.index;

import static org.junit.Assert.assertEquals;

import java.util.List;
import org.junit.Test;

public class OrderedListMapTest {

    @Test
    public void index() {
        OrderedListMap<String, String> subject = new OrderedListMap<>();

        subject.put("A", List.of("X", "Y", "Z"));

        assertEquals(0, subject.index("A", "X"));
        assertEquals(1, subject.index("A", "Y"));
        assertEquals(2, subject.index("A", "Z"));
        assertEquals(-1, subject.index("Ø", "X"));
        assertEquals(-2, subject.index("A", "Ø"));
    }
}