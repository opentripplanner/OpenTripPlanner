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

package org.opentripplanner.routing.carspeed;

import java.util.Calendar;
import java.util.TimeZone;

import junit.framework.TestCase;

public class TestWeekBaseline extends TestCase {

    /**
     * Test week offset computation.
     */
    public void testWeekOffset() throws Exception {

        Calendar gmtCal = Calendar.getInstance(TimeZone.getTimeZone("GMT"));

        gmtCal.clear();
        gmtCal.set(Calendar.YEAR, 2015);
        gmtCal.set(Calendar.MONTH, Calendar.JUNE);
        gmtCal.set(Calendar.DAY_OF_MONTH, 1); // Monday
        gmtCal.set(Calendar.HOUR_OF_DAY, 0);
        gmtCal.set(Calendar.MINUTE, 0);
        gmtCal.set(Calendar.SECOND, 0);
        gmtCal.set(Calendar.MILLISECOND, 0);

        int offset = WeekBaselineCarSpeedProvider.getWeekOffsetSec(gmtCal.getTimeInMillis());
        assertEquals(0, offset);

        gmtCal.add(Calendar.SECOND, 1);
        offset = WeekBaselineCarSpeedProvider.getWeekOffsetSec(gmtCal.getTimeInMillis());
        assertEquals(1, offset);

        gmtCal.add(Calendar.SECOND, -1);
        gmtCal.add(Calendar.DATE, 1);
        offset = WeekBaselineCarSpeedProvider.getWeekOffsetSec(gmtCal.getTimeInMillis());
        assertEquals(24 * 60 * 60, offset);

        gmtCal.add(Calendar.DATE, 6);
        gmtCal.add(Calendar.SECOND, -1);
        offset = WeekBaselineCarSpeedProvider.getWeekOffsetSec(gmtCal.getTimeInMillis());
        assertEquals(7 * 24 * 60 * 60 - 1, offset);

        gmtCal.add(Calendar.SECOND, 1);
        offset = WeekBaselineCarSpeedProvider.getWeekOffsetSec(gmtCal.getTimeInMillis());
        assertEquals(0, offset);
    }

}
