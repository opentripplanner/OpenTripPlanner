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

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.text.SimpleDateFormat;

/**
 * String Constants - related to date
 * 
 * @author Frank Purcell
 * @version $Revision: 1.0 $
 * @since 1.0
 */
public interface DateConstants {

    public static final List<String> DF_LIST = Collections.unmodifiableList(Arrays.asList(new String[] { 
            "yyyy.MM.dd.HH.mm.ss", "yyyy.MM.dd.HH.mm",
            "yyyy.MM.dd.HH.mm.ss.SS", "M.d.yy h.mm a", "M.d.yyyy h.mm a", "M.d.yyyy h.mma",
            "M.d.yyyy h.mm", "M.d.yyyy k.mm", "M.d.yyyy", "yyyy.M.d", "h.mm a"
    // NOTE: don't change the order of these strings...the simplest should be on the
    // bottom...you risk parsing the wrong thing (and ending up with year 0012)
    }));

    public static final List<String> SMALL_DF_LIST = Collections.unmodifiableList(
            Arrays.asList(new String[] { "M.d.yy", "yy.M.d", "h.mm a" }));

    // from apache date utils
    public static final String ISO_DATETIME_TIME_ZONE_FORMAT = "yyyy-MM-dd'T'HH:mm:ssZZ";

    // milli second times
    public static final Long ONE_WEEK_MILLI = 604800000L;
    public static final Long ONE_DAY_MILLI = 86400000L;
    public static final Long ONE_HOUR_MILLI = 3600000L;
    public static final Long ONE_MINUTE_MILLI = 60000L;
    public static final Long THIRTY_SECOND_MILLI = 30000L;
    public static final Long ONE_SECOND_MILLI = 1000L;
    public static final Long TEN_MINUTES_MILLI = ONE_MINUTE_MILLI * 10;
    public static final Long THIRTY_MINUTES_MILLI = ONE_MINUTE_MILLI * 30;
    public static final Long FORTY_5_MINUTES_MILLI = ONE_MINUTE_MILLI * 45;
    public static final Long TWELVE_HOUR_MILLI = ONE_HOUR_MILLI * 12;
    public static final Integer ONE_DAY_SECONDS = 24 * 60 * 60;

    public static final Date NOW = new Date();
    public static final Date NEXT_WEEK = new Date(NOW.getTime() + ONE_WEEK_MILLI);
    public static final Date NEXT_MONTH = new Date(NOW.getTime() + (ONE_WEEK_MILLI * 4)
            + (ONE_DAY_MILLI * 2));

    public static final String SIMPLE_TIME_FORMAT = "h:mm a";
    public static final String TIME_FORMAT = "hh:mm:ss a";
    public static final String DATE_FORMAT = "MM-dd-yyyy";
    public static final String DATE_TIME_FORMAT = "M.d.yy_k.m";
    public static final String DATE_TIME_FORMAT_NICE = "MM.dd.yyyy 'at' h:mm:a z";
    public static final String PRETTY_DATE_FORMAT = "MMMM d, yyyy";
    public static final String PRETTY_DT_FORMAT = PRETTY_DATE_FORMAT + " 'at' h:mm a z";
    public final static String DT_FORMAT = "M.d.yyyy h:mm a";
    public static final SimpleDateFormat dateSDF = new SimpleDateFormat(DATE_FORMAT);
    public static final SimpleDateFormat timeSDF = new SimpleDateFormat(TIME_FORMAT);
    public static final SimpleDateFormat simpTimeSDF = new SimpleDateFormat(SIMPLE_TIME_FORMAT);
    public static final SimpleDateFormat dateTimeSDF = new SimpleDateFormat(DATE_TIME_FORMAT);
    public static final SimpleDateFormat PRETTY_DATE = new SimpleDateFormat(PRETTY_DATE_FORMAT);
    public static final SimpleDateFormat PRETTY_DT = new SimpleDateFormat(PRETTY_DT_FORMAT);
    public static final SimpleDateFormat YEAR = new SimpleDateFormat("yyyy");
    public static final SimpleDateFormat dowSDF = new SimpleDateFormat("E");
    public static final SimpleDateFormat standardDateSDF = new SimpleDateFormat("M-d-yyyy");

    public static final String DATE = "date";
    public static final String EFFECTIVE_DATE = "effectiveDate";
    public static final String TODAY = "today";
    public static final String TIME = "time";
    public static final String HOUR = "Hour";
    public static final String MINUTE = "Minute";
    public static final String AM_PM = "AmPm";
    public static final String MONTH = "Month";
    public static final String DAY = "Day";

    public static final String WEEK = "week";
    public static final String SAT = "sat";
    public static final String SUN = "sun";
    public static final String AM = "am";
    public static final String PM = "pm";
}
