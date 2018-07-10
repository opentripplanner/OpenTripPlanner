/* This program is free software: you can redistribute it and/or
 modify it under the terms of the GNU Lesser General Public License
 as published by the Free Software Foundation, either version 3 of
 the License, or (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>. */

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
