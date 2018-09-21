package org.opentripplanner.routing.trippattern;

import junit.framework.TestCase;

public class DrtTravelTimeTest extends TestCase {

    public void testConstant() {
        DrtTravelTime tt = DrtTravelTime.fromSpec("20");
        assertEquals(0d, tt.getCoefficient());
        assertEquals(1200d, tt.getConstant());
        assertEquals(1200d, tt.process(100));
    }

    public void testCoefficient() {
        DrtTravelTime tt = DrtTravelTime.fromSpec("3t");
        assertEquals(3d, tt.getCoefficient());
        assertEquals(0d, tt.getConstant());
        assertEquals(180d, tt.process(60d));
    }

    public void testArithmeticFunction() {
        DrtTravelTime tt = DrtTravelTime.fromSpec("2.5t+5");
        assertEquals(2.5, tt.getCoefficient());
        assertEquals(300d, tt.getConstant());
        assertEquals(1800d, tt.process(600));
    }

    public void testTwoDigitConstant() {
        DrtTravelTime tt = DrtTravelTime.fromSpec("1t+12");
        assertEquals(1d, tt.getCoefficient());
        assertEquals(720d, tt.getConstant());
        assertEquals(1320d, tt.process(600));
    }

    public void testDecimalsEverywhere() {
        DrtTravelTime tt = DrtTravelTime.fromSpec("3.0t+5.00");
        assertEquals(3.0, tt.getCoefficient());
        assertEquals(300.0, tt.getConstant());
        assertEquals(2100d, tt.process(600));
    }

    public void testLarge() {
        DrtTravelTime tt = DrtTravelTime.fromSpec("2880.0");
        assertEquals(0.0, tt.getCoefficient());
        assertEquals(172800d, tt.getConstant());
        assertEquals(172800d, tt.process(20));
        assertEquals(172800d, tt.process(88));
    }

    public void testBadSpec() {
        try {
            DrtTravelTime.fromSpec("not to spec");
            fail( "Missing exception");
        } catch(IllegalArgumentException e) {
            assertEquals( e.getMessage(), DrtTravelTime.ERROR_MSG);
        }
    }

}
