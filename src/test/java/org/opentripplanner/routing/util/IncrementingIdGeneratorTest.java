package org.opentripplanner.routing.util;

import org.junit.Test;

import static org.junit.Assert.assertEquals;


public class IncrementingIdGeneratorTest {
    
    @Test
    public void testConstruct() {
        UniqueIdGenerator<String> gen = new IncrementingIdGenerator<String>();
        assertEquals(0, gen.getId(""));
        assertEquals(1, gen.getId("fake"));
        assertEquals(2, gen.getId("foo"));
    }
    
    @Test
    public void testConstructWithStart() {
        int start = 102;
        UniqueIdGenerator<String> gen = new IncrementingIdGenerator<String>(start);
        assertEquals(start, gen.getId(""));
        assertEquals(start + 1, gen.getId("fake"));
        assertEquals(start + 2, gen.getId("foo"));
    }
}