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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.opentripplanner.util.DateConstants.ONE_HOUR_MILLI;
import static org.opentripplanner.util.DateUtils.parseDate;

import java.text.DateFormatSymbols;
import java.text.NumberFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.ZoneId;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

public class DateUtilsTest {
    /**
     * Use a maximum timezone offset for testing to miximize rolling over to a new date.
     * For example at 06:01 Jan 1st UTC the date in timezone +18:00 will already be Jan 2nd.
    */
    private static final int TZ_OFFSET_18 = 18;
    private static final TimeZone TZ_P18 = TimeZone.getTimeZone(ZoneId.of("+" + TZ_OFFSET_18 + ":00"));
    private static final TimeZone UTC = TimeZone.getTimeZone("UTC");
    private SimpleDateFormat dateFormatMillis = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
    private SimpleDateFormat dateFormatUTC = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss Z");
    private SimpleDateFormat dateFormatTZ_ANY = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss Z");

    private static final Calendar T2018_01_15_1235_UTC = cal2018_01_15(12, 35, UTC);

    @Before
    public void setUp() {
        dateFormatMillis.setTimeZone(UTC);
        dateFormatUTC.setTimeZone(UTC);
        dateFormatTZ_ANY.setTimeZone(TZ_P18);
    }

    @Test
    public final void toDate_1970_01_01_0000_UTC() {
        Date date = DateUtils.toDate("1970-01-01", "00:00", UTC);
        assertEquals(0, date.getTime());
    }

    @Test
    public final void toDate_1970_01_01_0000_ANY_TIMEZONE() {
        Date date = DateUtils.toDate("1970-01-01", "00:00", TZ_P18);
        assertEquals(-(TZ_OFFSET_18 * ONE_HOUR_MILLI), date.getTime());
    }

    @Test
    public void toDate_with_DateAndTime_UTC() {
        Date date = DateUtils.toDate("1980-02-28", "13:45:30", UTC);
        assertEquals("1980-02-28 13:45:30 +0000", dateFormatUTC.format(date));
    }

    @Test
    public void toDate_with_DateAndTime_AnyTimeZone() {
        Date date = DateUtils.toDate("1980-02-28", "17:02:03", TZ_P18);
        assertEquals("1980-02-28 17:02:03 +1800", dateFormatTZ_ANY.format(date));
    }

    @Test
    public final void testToDate_withNoDate_UTC() {
        withFixedTime(T2018_01_15_1235_UTC, () -> {
            Date date = DateUtils.toDate(null, "13:45:30", UTC);
            assertEquals("2018-01-15 13:45:30 +0000", dateFormatUTC.format(date));
        });
    }

    @Test
    public final void testToDate_withNoDate_AnyTimeZone() {
        withFixedTime(cal2018_01_15(0, 1, TZ_P18), () -> {
            Date date = DateUtils.toDate(null, "13:45:30", TZ_P18);
            assertEquals("2018-01-15 13:45:30 +1800", dateFormatTZ_ANY.format(date));
        });
    }

    @Test
    @Ignore("FIXME: This fails for the current implementation. The default should be to use the current time of the " +
            "provided timezone.")
    public final void testToDate_withNoTime_UTC() {
        withFixedTime(T2018_01_15_1235_UTC, () -> {
            Date date = DateUtils.toDate("2017-02-28", null, UTC);
            assertEquals("2017-02-28 12:35:00 +0000", dateFormatUTC.format(date));
        });
    }

    @Test
    @Ignore("FIXME: This fails for the current implementation. The default should be to use the current time of the " +
            "provided timezone.")
    public final void testToDate_withNoTime_AnyTimeZone() {
        withFixedTime(T2018_01_15_1235_UTC, () -> {
            Date date = DateUtils.toDate("2017-02-28", null, TZ_P18);
            assertEquals("2017-02-28 12:35:00 +0000", dateFormatUTC.format(date));
        });
    }

    @Test
    public final void testParseDate() {
        // Normal case
        assertDate(parseDate("2017-02-28", UTC), "2017-02-28 00:00:00.000");
        // TimeZone +18:00
        assertDate(parseDate("2017-02-28", TZ_P18), "2017-02-27 06:00:00.000");
        // Different separators: _ - : . /
        assertDate(parseDate("2017_02_28", UTC), "2017-02-28 00:00:00.000");
        assertDate(parseDate("2017:02:28", UTC), "2017-02-28 00:00:00.000");
        assertDate(parseDate("2017.02.28", UTC), "2017-02-28 00:00:00.000");
        assertDate(parseDate("2017/02/28", UTC), "2017-02-28 00:00:00.000");
        // With time:
        assertDate(parseDate("2017-02-28_12:30:59", UTC), "2017-02-28 12:30:59.000");
        assertDate(parseDate("2017-02-28_12:30", UTC), "2017-02-28 12:30:00.000");
        assertDate(parseDate("2017-02-28 12:30", UTC), "2017-02-28 00:00:00.000");        // Space as a delimiter for dat and time
        assertDate(parseDate("2017-02-28T12:30", UTC), "2017-02-28 00:00:00.000");        // ISO 8601 not supported
        assertDate(parseDate("2017-02-28_12:30:59.999", UTC), "2017-02-28 12:30:59.000"); // Milliseconds?
        assertDate(parseDate("2017-02-28_12:30:59,999", UTC), "2017-02-28 12:30:59.000"); // Milliseconds?
        assertDate(parseDate("02/28/17 16:30 pm", UTC), null);                            //
        assertDate(parseDate("02/28/2017 12:30 pm", UTC), "2017-02-28 12:30:00.000");
        assertDate(parseDate("02/28/2017 12:10 am", UTC), "2017-02-28 00:10:00.000");
        assertDate(parseDate("02/28/2017 01:30am", UTC), "2017-02-28 01:30:00.000");
        assertDate(parseDate("02/28/2017 12:30", UTC), "2017-02-28 00:30:00.000");        // Hours ?
        assertDate(parseDate("02/28/2017 24:30", UTC), "2017-02-28 00:30:00.000");        // Hour 24 accepted !!
        assertDate(parseDate("02/28/2017 26:30", UTC), "2017-02-28 00:00:00.000");        // Hour 26 -> Time ignored
        assertDate(parseDate("02/28/2017 13;30", UTC), "2017-02-28 00:00:00.000");        // illegal char ';' -> Time ignored
        assertDate(parseDate("02/28/2017 13;30", UTC), "2017-02-28 00:00:00.000");        // Time ignored
        assertDate(parseDate("2_3_2000", UTC), "2000-02-03 00:00:00.000");
        assertDate(parseDate("2_3_1000", UTC), "1000-02-03 00:00:00.000");
        assertDate(parseDate("2017-02-03", UTC), "2017-02-03 00:00:00.000");
        assertDate(parseDate("2017-2-3", UTC), "2017-02-03 00:00:00.000");
        assertDate(parseDate("1:15 am", UTC), "1970-01-01 01:15:00.000");                // date default to 1970-01-01 ?

        // Norwegian
        assertDate(parseDate("24.12.2017_10:00", UTC), null);                             // Time ignored
        assertDate(parseDate("11.12.2017_10:00", UTC), "2017-11-12 00:00:00.000");        // Time ignored
    }


    /* private methods */

    private void assertDate( Date actual, String expected) {
        if(actual == null) {
            assertNull("No date expected", expected);
        }
        else {
            assertEquals(expected, dateFormatMillis.format(actual));
        }
    }

    private static Calendar cal2018_01_15(int hour, int minute, TimeZone tz) {
        Calendar cal = Calendar.getInstance(tz);
        cal.set(2018, Calendar.JANUARY, 15, hour, minute);
        cal.clear(Calendar.SECOND);
        cal.clear(Calendar.MILLISECOND);
        return cal;
    }

    private void withFixedTime(Calendar calTime, Runnable body) {
        DateUtils.withFixedTime(calTime.getTimeInMillis(), body);
    }
}
