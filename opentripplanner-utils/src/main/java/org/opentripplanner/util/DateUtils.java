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

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * 
 * @author Frank Purcell (p u r c e l l f @ t r i m e t . o r g)
 * @date October 20, 2009
 */
public class DateUtils implements DateConstants {

    private static final Logger LOG = LoggerFactory.getLogger(DateUtils.class);

    /**
     * Returns a Date object based on input date & time parameters Defaults to today / now (when
     * date / time are null)
     * 
     * @param date
     * @param time
     */
    static public Date toDate(String date, String time, TimeZone tz) {
        Date retVal = new Date();
        if (date != null) {
            Date d = parseDate(date, tz);
            Integer s = null;
            if (time != null)
                s = secPastMid(time);
            if (s == null || s < 0)
                s = secPastMid();
            if (d != null && s != null)
                retVal = new Date(d.getTime() + s * 1000);
        } else if (time != null) {
            Integer s = secPastMid(time);
            if (s != null && s > 0) {
                // ??? replace String manipulation with Calendar.set hack or JodaTime (AMB)
                String p = formatDate(DATE_FORMAT, retVal, tz);
                Date d = parseDate(p, tz);
                retVal = new Date(d.getTime() + s * 1000);
            }
        }

        return retVal;
    }

    // TODO: could be replaced with Apache's DateFormat.parseDate ???
    static public Date parseDate(String input, TimeZone tz) {
        Date retVal = null;

        try {
            String newString = input.trim().replace('_', '.').replace('-', '.').replace(':', '.').replace(
                    '/', '.');
            if (newString != null) {
                List<String> dl = DF_LIST;

                // if it looks like we have a small date format, ala 11.4.09, then use another set
                // of compares
                if (newString.length() <= 8 && !newString.matches(".*20\\d\\d.*")) {
                    dl = SMALL_DF_LIST;
                }

                for (String df : dl) {
                    SimpleDateFormat sdf = new SimpleDateFormat(df);
                    sdf.setTimeZone(tz);
                    retVal = DateUtils.parseDate(sdf, newString);
                    // getYear() returns (year - 1900)
                    if (retVal != null && retVal.getYear() + 1900 >= 2000)
                        break;
                }
            }
        } catch (Exception _) {
            throw new RuntimeException("Could not parse " + input);
        }

        return retVal;
    }

    public static Integer getDurationInMinutes(String timeA, String timeB) {
        return getDuration(timeA, timeB) / 60;
    }

    public static Integer getDuration(String timeA, String timeB) {
        int a = secPastMid(timeA);
        int b = secPastMid(timeB);

        if (b < a) {
            b = b + ONE_DAY_SECONDS;
        }

        return b - a;
    }

    // ///////// SECONDS PAST MIDNIGHT ////////////////////////

    public static int secPastMid() {
        return secPastMid(new Date());
    }

    synchronized public static int secPastMid(Date dateTime) {
        String t = simpTimeSDF.format(dateTime);
        return secPastMid(t);
    }

    public static int secPastMid(String time) {
        int retVal = 0;

        Integer tmp = secondsPastMidnight(time);
        if (tmp != null)
            retVal = tmp;

        return retVal;
    }

    /**
     * Convert HH:MM or HH:MM:SS to seconds past midnight (note, spm format is chosen for
     * comptability with other data feeds)
     * 
     * @param Time
     *            in HH:MM / HH:MM:SS format
     * @return integer seconds past midnight
     */
    public static Integer secondsPastMidnight(String time) {
        Integer retVal = null;

        boolean amPm = false;
        int addHours = 0;
        int hour = 0, min = 0, sec = 0;
        try {
            String[] hms = time.toUpperCase().split(":");

            // if we don't have a colon sep string, assume string is int and represents seconds past
            // midnight
            if (hms.length < 2)
                return getIntegerFromString(time);

            if (hms[1].endsWith("PM") || hms[1].endsWith("AM")) {
                amPm = true;

                if (hms[1].contains("PM"))
                    addHours = 12;

                int suffex = hms[1].lastIndexOf(' ');
                if (suffex < 1) {
                    suffex = hms[1].lastIndexOf("AM");
                    if (suffex < 1) {
                        suffex = hms[1].lastIndexOf("PM");
                    }
                }
                hms[1] = hms[1].substring(0, suffex);
            }

            int h = Integer.parseInt(trim(hms[0]));
            if (amPm && h == 12)
                h = 0;
            hour = h + addHours;

            min = Integer.parseInt(trim(hms[1]));
            if (hms.length > 2) {
                sec = Integer.parseInt(trim(hms[2]));
            }

            retVal = (hour * 60 * 60) + (min * 60) + sec;
        } catch (Exception e) {
            LOG.info(time + " didn't parse", e);
            retVal = null;
        }

        return retVal;
    }

    public static int getIntegerFromString(String input) {
        try {
            return new Integer(input);
        } catch (Exception e) {
            return 0;
        }
    }

    public static long getTimeInMillis(String time) {
        Integer secs = DateUtils.secondsPastMidnight(time);
        if (secs == null) {
            secs = 0;
            try {
                int isPM = 0;

                String[] t = time.split("[\\W]+");
                if (t.length >= 3 && t[2].trim().equalsIgnoreCase("PM"))
                    isPM = 12;

                secs = (Integer.valueOf(t[0]) + isPM) * 60 * 60; // hours
                secs += Integer.valueOf(t[1]) * 60; // minutes in seconds
            } catch (Exception _) {
            }
        }

        return secs * 1000; // milliseconds
    }

    /** gets a pretty time (eg: h:mm a) from seconds past midnight */
    public static String getTime(int time) {
        String retVal = secondsToString(time) + " " + getAmPm(time);
        return retVal;
    }

    /**
     * Converts time in seconds to a <code>String</code> in the format h:mm.
     * 
     * @param time
     *            the time in seconds.
     * @return a <code>String</code> representing the time in the format h:mm
     */
    public static String secondsToString(int time) {
        return secondsToString(time, false);
    }

    public static String secondsToString(int time, boolean withAmPm) {
        if (time < 0)
            return null;

        String minutesStr = secondsToMinutes(time);
        String hoursStr = secondsToHour(time);
        String amPmStr = withAmPm ? getAmPm(time) : "";

        return hoursStr + ":" + minutesStr + amPmStr;
    }

    public static String secondsToHour(int time) {
        if (time < 0)
            return null;
        int hours = (time / 3600) % 12;
        String hoursStr = hours == 0 ? "12" : hours + "";
        return hoursStr;
    }

    public static String secondsToMinutes(int time) {
        if (time < 0)
            return null;

        int minutes = (time / 60) % 60;
        String minutesStr = (minutes < 10 ? "0" : "") + minutes;
        return minutesStr;
    }

    public static String getAmPm(int time) {
        return getAmPm(time, AM, PM);
    }

    public static String getAmPm(int time, String am, String pm) {
        if (time % 86400 >= 43200)
            return pm;
        else
            return am;
    }

    // /////////////////////////////

    public static String trim(String str) {
        String retVal = str;
        try {
            retVal = str.trim();
            retVal = retVal.replaceAll("%20;", "");
            retVal = retVal.replaceAll("%20", "");
        } catch (Exception _) {
        }
        return retVal;
    }

    public static String formatDate(String sdfFormat, Date date, TimeZone tz) {
        return formatDate(sdfFormat, date, null, tz);
    }

    public static String formatDate(String sdfFormat, Date date, String defValue, TimeZone tz) {
        String retVal = defValue;
        try {
            SimpleDateFormat sdf = new SimpleDateFormat(sdfFormat);
            sdf.setTimeZone(tz);
            retVal = sdf.format(date);
        } catch (Exception e) {
            retVal = defValue;
        }

        return retVal;
    }

    public static Date parseDate(String sdf, String string) {
        return parseDate(new SimpleDateFormat(sdf), string);
    }

    public synchronized static Date parseDate(SimpleDateFormat sdf, String string) {
        sdf.setLenient(false);
        try {
            return sdf.parse(string);
        } catch (Exception e) {
            // log.debug(string + " was not recognized by " + format.toString());
        }
        return null;
    }
    
    public static long absoluteTimeout(double relativeTimeoutSeconds) {
        if (relativeTimeoutSeconds <= 0)
            return Long.MAX_VALUE;
        else
            return System.currentTimeMillis() + (long)(relativeTimeoutSeconds * 1000.0);
    }
}
