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

package org.opentripplanner.util;

import static org.junit.Assert.assertEquals;

import java.util.Date;
import java.util.TimeZone;

import org.junit.Test;

public class DateUtilsTest {
    @Test
    public final void testToDate() {
        Date date = DateUtils.toDate("1970-01-01", "00:00", TimeZone.getTimeZone("UTC"));
        assertEquals(0, date.getTime());

        date = DateUtils.toDate(null, "00:00", TimeZone.getTimeZone("UTC"));
        assertEquals(0, date.getTime() % DateUtils.ONE_DAY_MILLI);
    }
}
