package org.opentripplanner.profile;

import org.junit.Test;

import junit.framework.AssertionFailedError;
import junit.framework.TestCase;

/** test that distributions and convolution work as expected */
public class DistributionTest extends TestCase {
    @Test
    public void testConvolutionNoOffset () {
        Distribution d1 = Distribution.uniform(20);
        Distribution d2 = Distribution.uniform(20);
        
        assertFuzzyEquals(Integer.MAX_VALUE, d1.integrate(0, 19));
        
        // convolute the distributions
        Distribution c = d1.convolve(d2);
        
        assertFuzzyEquals(Integer.MAX_VALUE, c.integrate(0, 38));
        
        // make sure it worked as expected
        assertFuzzyEquals(Distribution.multiply(d1.get(0), d2.get(0)), c.get(0));
        assertFuzzyEquals(Distribution.multiply(d1.get(0), d2.get(0)), c.get(38));
        assertFuzzyEquals(Distribution.multiply(d1.get(0), d2.get(1)) * 2, c.get(1));
        assertFuzzyEquals(Distribution.multiply(d1.get(0), d2.get(1)) * 2, c.get(37));
    }
    
    @Test
    public void testConvolutionWithOffset () {
        Distribution d1 = Distribution.uniform(20);
        d1.offset = 10;
        Distribution d2 = Distribution.uniform(10);
        d2.offset = 20;
        
        Distribution c = d1.convolve(d2);
        
        assertEquals(30, c.offset);
        assertFuzzyEquals(Distribution.multiply(d1.get(10), d2.get(20)), c.get(30));
        assertFuzzyEquals(Distribution.multiply(d1.get(10), d2.get(20)), c.get(58));
        assertFuzzyEquals(Distribution.multiply(d1.get(10), d2.get(20)) * 2, c.get(31));
        assertFuzzyEquals(Distribution.multiply(d1.get(10), d2.get(20)) * 2, c.get(57));
    }
    
    // this test always fails but will print out how many convolutions you get per second.
    /*@Test
    public void testConvolutionPerformance () {
        Distribution d1 = Distribution.uniform(1200);
        d1.offset = 1200;
        Distribution c = d1;
        
        long now = System.currentTimeMillis();
        
        for (int i = 0;; i++) {
            if ( System.currentTimeMillis() > now + 10000)
                assertTrue(i + " iterations took 10s", false);
            
            d1.convolve(d1);
        }
    }*/
    
    public void assertFuzzyEquals (int expected, int actual) {
        if (Math.abs(expected - actual) > 1000) {
            throw new AssertionFailedError("expected: " + expected + " but was " + actual);
        }
    }
}
