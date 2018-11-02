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
