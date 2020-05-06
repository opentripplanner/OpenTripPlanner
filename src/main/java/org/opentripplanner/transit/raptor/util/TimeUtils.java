package org.opentripplanner.transit.raptor.util;


import java.time.Duration;
import java.util.Calendar;


/**
 * Time utility methods. See the unit test for examples on how to use this class.
 */
public class TimeUtils {
    /**
     * This constant is used to represent a service time, which is not set.
     * This typically is used in the requestWhen an integer is used to represent a service time in seconds
     */
    public static final int NOT_SET = -1_000_000;

    private enum FormatType { COMPACT, LONG, SHORT, DURATION }
    private static final boolean USE_RAW_TIME = false;


    /** This is a utility class. Do not instantiate this class. It should have only static methods. */
    private TimeUtils() { }

    public static int hms2time(int hour, int minute, int second) {
        return second + 60 * (minute + (60 * hour));
    }

    public static int hm2time(int hour, int minute) {
        return hms2time(hour, minute, 0);
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


    public static String timeToStrCompact(int time) {
        return timeToStrCompact(time, -1);
    }

    public static String timeToStrCompact(int time, int notSetValue) {
        return timeStr(time, notSetValue, FormatType.COMPACT);
    }

    public static String timeToStrCompact(Calendar time) {
        return timeStr(time, FormatType.COMPACT);
    }

    public static String durationToStr(Duration duration) {
        return durationToStr((int)duration.toSeconds());
    }

    public static String durationToStr(int timeSeconds) {
        return durationToStr(timeSeconds, NOT_SET);
    }

    public static String durationToStr(int timeSeconds, int notSetValue) {
        return timeStr(timeSeconds, notSetValue, FormatType.DURATION);
    }

    public static String msToSecondsStr(long timeMs) {
        if(timeMs == 0) { return "0 seconds"; }
        if(timeMs == 1000) { return "1 second"; }
        if(timeMs < 100) { return String.format ("%.3f seconds",  timeMs/1000.0); }
        if(timeMs < 995) { return String.format ("%.2f seconds",  timeMs/1000.0); }
        if(timeMs < 9950) { return String.format ("%.1f seconds",  timeMs/1000.0); }
        else { return String.format ("%.0f seconds",  timeMs/1000.0); }
    }

    public static String timeToStrLong(int time) {
        return timeToStrLong(time, -1);
    }

    public static String timeToStrLong(int time, int notSetValue) {
        return timeStr(time, notSetValue, FormatType.LONG);
    }

    public static String timeToStrLong(Calendar time) {
        return timeStr(time, FormatType.LONG);
    }

    public static String timeToStrShort(int time) {
        return timeStr(time, NOT_SET, FormatType.SHORT);
    }

    public static String timeToStrShort(Calendar time) {
        return timeStr(time, FormatType.SHORT);
    }

    public static Calendar midnightOf(Calendar time) {
        final Calendar midnight = (Calendar) time.clone();
        midnight.set(Calendar.HOUR, 0);
        midnight.set(Calendar.MINUTE, 0);
        midnight.set(Calendar.SECOND, 0);
        midnight.set(Calendar.MILLISECOND, 0);
        return midnight;
    }


    /* private methods */

    private static String timeStr(int time, int notSetValue, FormatType formatType) {
        if(time == notSetValue) {
            return "";
        }
        if(USE_RAW_TIME) {
            return Integer.toString(time);
        }
        boolean sign = time < 0;
        time = Math.abs(time);

        int sec = time % 60;
        time =  time / 60;
        int min = time % 60;
        int hour = time / 60;

        return timeStr(sign, hour, min, sec, formatType);
    }

    private static String timeStr(Calendar time, FormatType formatType) {
        if(time == null) {
            return "";
        }
        int sec = time.get(Calendar.SECOND);
        int min = time.get(Calendar.MINUTE);
        int hour = time.get(Calendar.HOUR_OF_DAY);

        return timeStr(false, hour, min, sec, formatType);
    }

    private static String timeStr(
            boolean sign, int hours, int min, int sec, FormatType formatType
    ) {
        switch (formatType) {
            case LONG: return sign(sign, timeStrLong(hours, min, sec));
            case SHORT: return sign(sign, timeStrShort(hours, min));
            case DURATION: return sign(sign, timeStrDuration(hours, min, sec));
            default: return sign(sign, timeStrCompact(hours, min, sec));
        }
    }

    private static String sign(boolean sign , String value) {
        return sign ? "-" + value : value;
    }

    private static String timeStrCompact(int hour, int min, int sec) {
        return sec == 0
                ? String.format("%d:%02d", hour, min)
                : String.format("%d:%02d:%02d", hour, min, sec);
    }

    private static String timeStrLong(int hour, int min, int sec) {
        return String.format("%02d:%02d:%02d", hour, min, sec);
    }

    private static String timeStrShort(int hour, int min) {
        return String.format("%02d:%02d", hour, min);
    }

    /** Examples: "3d4h3m4s", "3m", "59s", "1h3s" */
    private static String timeStrDuration(int hours, int min, int sec) {
        int days = hours/24;
        hours = hours % 24;
        String buf = "";

        if(days != 0) { buf += days + "d"; }
        if(hours != 0) { buf += hours + "h"; }
        if(min != 0) { buf += min + "m"; }
        if(sec != 0) { buf += sec + "s"; }
        return buf.isBlank() ? "0s" : buf;
    }
}
