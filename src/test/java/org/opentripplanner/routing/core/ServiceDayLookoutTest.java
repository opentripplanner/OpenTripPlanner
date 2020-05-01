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
package org.opentripplanner.routing.core;

import org.junit.Test;
import org.opentripplanner.GtfsTest;
import org.opentripplanner.api.model.Leg;
import org.opentripplanner.util.TestUtils;

public class ServiceDayLookoutTest extends GtfsTest {

    @Override
    public String getFeedName() {
        return "vermont/ruralcommunity-flex-vt-us.zip";
    }

    // Kingdom Shopper 2 runs 2nd and 4th Wednesday of every month

    @Test
    public void testLookoutBaseline() {
        long time = TestUtils.dateInSeconds("America/New_York", 2018, 5, 25, 8, 0, 0); // monday
        Leg leg = plan(time, "804108", "804106", null, false, false,
                null, null, null);
        assertNull(leg);
    }

    @Test
    public void testLookout() {
        long time = TestUtils.dateInSeconds("America/New_York", 2018, 5, 25, 8, 0, 0); // monday
        RoutingRequest opt = new RoutingRequest();
        opt.serviceDayLookout = 2;
        Leg leg = plan(time, "804108", "804106", null, false, false,
                null, null, null, 1, opt)[0];
        assertNotNull(leg);
        assertEquals("3116", leg.routeId.getId());
    }

    @Test
    public void testLookoutBaselineArriveBy() {
        long time = TestUtils.dateInSeconds("America/New_York", 2018, 5, 25, 8, 0, 0); // monday
        Leg leg = plan(-1 * time, "804108", "804106", null, false, false,
                null, null, null);
        assertNull(leg);
    }

    @Test
    public void testLookoutArriveBy() {
        long time = TestUtils.dateInSeconds("America/New_York", 2018, 5, 25, 8, 0, 0); // monday
        RoutingRequest opt = new RoutingRequest();
        opt.serviceDayLookout = 14; // look back two weeks
        Leg leg = plan(-1 * time, "804108", "804106", null, false, false,
                null, null, null, 1, opt)[0];
        assertNotNull(leg);
        assertEquals("3116", leg.routeId.getId());
    }
}
