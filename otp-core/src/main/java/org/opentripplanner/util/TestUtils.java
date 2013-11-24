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

import java.util.GregorianCalendar;
import java.util.TimeZone;

public class TestUtils {
    public static final int JANUARY = 0;
    public static final int FEBRUARY = 1;
    public static final int MARCH = 2;
    public static final int APRIL = 3;
    public static final int MAY = 4;
    public static final int JUNE = 5;
    public static final int JULY = 6;
    public static final int AUGUST = 7;
    public static final int SEPTEMBER = 8;
    public static final int OCTOBER = 9;
    public static final int NOVEMBER = 10;
    public static final int DECEMBER = 11;

    public static long dateInSeconds(String timeZoneId,
            int year, int month, int day, int hour, int minute, int second) {
        TimeZone timeZone = TimeZone.getTimeZone(timeZoneId);
        GregorianCalendar calendar = new GregorianCalendar(year, month, day, hour, minute, second);
        calendar.setTimeZone(timeZone);
        return calendar.getTimeInMillis() / 1000;
    }

    public static long toSeconds(GregorianCalendar time) {
        return time.getTimeInMillis() / 1000;
    }
}
