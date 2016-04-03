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

package org.opentripplanner.routing.edgetype;

import org.opentripplanner.GtfsTest;
import org.opentripplanner.api.model.Leg;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.TimeZone;

public class PatternInterlineDwellTest extends GtfsTest {

    @Override
    public boolean isLongDistance() { return true; } // retrying wrecks the tests

    @Override
    public String getFeedName() {
        return "gtfs/interlining";
    }

    // TODO Allow using Calendar or ISOdate for testing, interpret it in the given graph's timezone.

    public void testInterlining() {
        Calendar calendar = new GregorianCalendar(2014, Calendar.JANUARY, 01, 00, 05, 00);
        calendar.setTimeZone(TimeZone.getTimeZone("America/New_York"));
        long time = calendar.getTime().getTime() / 1000;
        // We should arrive at the destination using two legs, both of which are on
        // the same route and with zero transfers.
        Leg[] legs = plan(time, "stop0", "stop3", null, false, false, null, null, null, 2);
        assertEquals(legs[0].routeId.getId(), "route1");
        assertEquals(legs[1].routeId.getId(), "route1");
        assertTrue(itinerary.transfers == 0);
    }

    // TODO test for trips on the same block with no transfer allowed (Trimet special case)

}
