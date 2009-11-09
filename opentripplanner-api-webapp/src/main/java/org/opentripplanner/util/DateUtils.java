package org.opentripplanner.util;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * 
 * 
 * @author Frank Purcell (p u r c e l l f @ t r i m e t . o r g)
 * @date October 20, 2009
 */
public class DateUtils implements DateConstants {

    private static final Logger LOGGER = Logger.getLogger(DateUtils.class.getCanonicalName());

    /**
     * Returns a Date object based on input date & time parameters Defaults to today / now (when
     * date / time are null)
     * 
     * @param date
     * @param time
     */
    static public Date toDate(String date, String time) {
        Date retVal = new Date();

        if (date != null) {
            Date d = parseDate(date);
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
                String p = formatDate(DATE_FORMAT, retVal);
                Date d = parseDate(p);
                retVal = new Date(d.getTime() + s * 1000);
            }
        }

        return retVal;
    }

    static public String todayAsString() {
        return formatDate(DATE_FORMAT, new Date());
    }

    static public String nowAsString() {
        return formatDate(TIME_FORMAT, new Date());
    }

    // TODO: could be replaced with Apache's DateFormat.parseDate ???
    static public Date parseDate(String input) {
        Date retVal = null;

        try {
            String newString = input.replace('_', '.').replace('-', '.').replace(':', '.').replace(
                    '/', '.');
            if (newString != null) {
                String[] dl = DF_LIST;

                // if it looks like we have a small date format, ala 11.4.09, then use another set
                // of compares
                if (newString.length() <= 8 && !newString.matches(".*20\\d\\d.*")) {
                    dl = SMALL_DF_LIST;
                }

                for (String df : dl) {
                    retVal = DateUtils.parseDate(new SimpleDateFormat(df), newString);
                    if (retVal != null)
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
            LOGGER.log(Level.INFO, time + " didn't parse", e);
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

        return new String(hoursStr + ":" + minutesStr + amPmStr);
    }

    public static String secondsToHour(int time) {
        if (time < 0)
            return null;
        int hours = (time / 3600) % 12;
        String hoursStr = hours == 0 ? "12" : hours + "";
        return new String(hoursStr);
    }

    public static String secondsToMinutes(int time) {
        if (time < 0)
            return null;

        int minutes = (time / 60) % 60;
        String minutesStr = (minutes < 10 ? "0" : "") + minutes;
        return new String(minutesStr);
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

    public static String formatDate(String sdfFormat, Date date) {
        return formatDate(sdfFormat, date, null);
    }

    public static String formatDate(String sdfFormat, Date date, String defValue) {
        String retVal = defValue;
        try {
            SimpleDateFormat sdf = new SimpleDateFormat(sdfFormat);
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
        sdf.setLenient(true);
        try {
            return sdf.parse(string);
        } catch (Exception e) {
            // log.debug(string + " was not recognized by " + format.toString());
        }
        return null;
    }
}
