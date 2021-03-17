package org.opentripplanner.util.time;


import javax.annotation.Nonnull;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * Time utility methods. See the unit test for examples on how to use this class.
 */
public class TimeUtils {
    /**
     * This constant is used to represent a service time, which is not set.
     * This typically is used in the requestWhen an integer is used to represent a service time in seconds
     */
    public static final int NOT_SET = -1_000_000;

    private static final Pattern DAYS_SUFFIX = Pattern.compile("([-+])(\\d+)d");


    /** This is a utility class. Do not instantiate this class. It should have only static methods. */
    private TimeUtils() { }


    public static int hms2time(int hour, int minute, int second) {
        return second + 60 * (minute + (60 * hour));
    }

    public static int hm2time(int hour, int minute) {
        return hms2time(hour, minute, 0);
    }


    /**
     * Parse a time into seconds past midnight.
     * Format accepted for time: HH, HH:MM and HH:MM:SS
     * In addition a day offset is allowed: (+|-)DDd. Examples:
     * <pre>
     *  00:00:00  =>  01:00:00
     *         1  =>  01:00:00
     *       1:2  =>  01:02:00
     *     1:2:3  =>  01:02:03
     *  13:59:59  =>  13:59:59
     *
     *  // Additional days (plus and minus)
     *         1+1d  =>  01:00:00+1d
     *  11:02:03-2d  =>  11:02:03-3d
     *
     *  // Negative values are supported
     *    -10:20     => -01:02:00
     * -13:59:59     => -13:59:59
     * -11:02:03-2d  => -(11:02:03-3d)  ==>  12:57:57+2d
     * </pre>
     */
    public static int time(String hhmmss) {
        int days = 0;
        String[] tokens;
        boolean negative = hhmmss.startsWith("-");

        if(negative) { hhmmss =  hhmmss.substring(1); }

        Matcher m = DAYS_SUFFIX.matcher(hhmmss);
        if (m.find()) {
            days = Integer.parseInt(m.group(2));
            if("-".equals(m.group(1))) { days = -days; }
            tokens = hhmmss.substring(0, m.start()).split(":");
        }
        else {
            tokens = hhmmss.split(":");
        }

        if(tokens.length > 3 || tokens.length < 1) {
            throw new IllegalStateException(
                "Parse error, not a valid time (HH|HH:MM|HH:MM:SS): '" + hhmmss + "'"
            );
        }
        int hh = Integer.parseInt(tokens[0]);
        if(hh > 23) {
            days += hh/24;
            hh = hh % 24;
        }
        int mm = tokens.length>1 ? Integer.parseInt(tokens[1]) : 0;
        int ss = tokens.length==3 ? Integer.parseInt(tokens[2]) : 0;
        int seconds = LocalTime.of(hh, mm, ss).toSecondOfDay() + days * DateConstants.ONE_DAY_SECONDS;
        return negative ? -seconds : seconds;
    }

    public static int parseHHMM(String hhmm, int defaultValue) {
        String[] tokens = hhmm.split(":");
        if(tokens.length != 2) {
            return defaultValue;
        }

        int hh = Integer.parseInt(tokens[0]);
        int mm = Integer.parseInt(tokens[1]);

        return hm2time(hh, mm);
    }

    /** pares time of format hh:mm:ss. */
    public static int parseTimeLong(String hhmmss, int defaultValue) {
        String[] tokens = hhmmss.split(":");
        if(tokens.length != 3) {
            return defaultValue;
        }

        int hh = Integer.parseInt(tokens[0]);
        int mm = Integer.parseInt(tokens[1]);
        int ss = Integer.parseInt(tokens[2]);

        return hms2time(hh, mm, ss);
    }

    /**
     * Parse a string of times like "00:20 01:20 05:57:30" into an array of local times.
     * This can be very helpful when specifying a schedule using a sting instead of using a
     * int array with seconds past midnight.
     */
    public static int[] times(@Nonnull String input) {
        return Arrays.stream(input.split("[ ,;]+"))
            .mapToInt(TimeUtils::time)
            .toArray();
    }


    public static String timeToStrCompact(int time) {
        return RelativeTime.ofSeconds(time).toCompactStr();
    }

    public static String timeToStrCompact(int time, int notSetValue) {
        return time == notSetValue ? "" : RelativeTime.ofSeconds(time).toCompactStr();
    }

    public static String timeToStrCompact(Calendar time) {
        return time == null ? "" : RelativeTime.from(time).toCompactStr();
    }

    public static String timeToStrLong(int time) {
        return RelativeTime.ofSeconds(time).toLongStr();
    }

    public static String timeToStrLong(int time, int notSetValue) {
        return time == notSetValue ? "" : RelativeTime.ofSeconds(time).toLongStr();
    }

    public static String timeToStrLong(Calendar time) {
        return RelativeTime.from(time).toLongStr();
    }

    public static Calendar midnightOf(Calendar time) {
        final Calendar midnight = (Calendar) time.clone();
        midnight.set(Calendar.HOUR, 0);
        midnight.set(Calendar.MINUTE, 0);
        midnight.set(Calendar.SECOND, 0);
        midnight.set(Calendar.MILLISECOND, 0);
        return midnight;
    }

    /**
     * This method take a date, a time in seconds and a zoneId and create a {@link ZonedDateTime}.
     * <p>
     * This method follow the GTFS specification for time: "The time is measured from
     * 'noon minus 12h' of the service day (effectively midnight except for days on which
     * daylight savings time changes occur." See https://developers.google.com/transit/gtfs/reference#field_types
     * <p>
     * Note! The itinerary uses the old Java Calendar, but we would like to migrate to the new java.time
     * library; Hence this method is already changed. To convert into the legacy Calendar use
     * {@link GregorianCalendar#from(ZonedDateTime)} method.
     *
     * @param date the "service" date
     * @param seconds number of seconds since noon minus 12 hours (midnight).
     */
    public static ZonedDateTime zonedDateTime(LocalDate date, int seconds, ZoneId zoneId) {
        return RelativeTime.ofSeconds(seconds).toZonedDateTime(date, zoneId);
    }
}
