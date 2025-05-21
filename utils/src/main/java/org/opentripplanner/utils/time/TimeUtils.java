package org.opentripplanner.utils.time;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Locale;
import java.util.Random;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Time utility methods. See the unit test for examples on how to use this class.
 */
public class TimeUtils {

  public static final Integer ONE_DAY_SECONDS = 24 * 60 * 60;
  private static final Pattern DAYS_SUFFIX = Pattern.compile("([-+])(\\d+)d");
  private static final AtomicLong BUSY_WAIT_GRACE_PERIOD_TIMEOUT = new AtomicLong(0);

  /** This is a utility class. Do not instantiate this class. It should have only static methods. */
  private TimeUtils() {}

  public static int hms2time(int hour, int minute, int second) {
    return second + 60 * (minute + (60 * hour));
  }

  public static int hm2time(int hour, int minute) {
    return hms2time(hour, minute, 0);
  }

  /**
   * Parse a time into seconds past midnight. Format accepted for time: HH, HH:MM and HH:MM:SS In
   * addition a day offset is allowed: (+|-)DDd. Examples:
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

    if (negative) {
      hhmmss = hhmmss.substring(1);
    }

    Matcher m = DAYS_SUFFIX.matcher(hhmmss);
    if (m.find()) {
      days = Integer.parseInt(m.group(2));
      if ("-".equals(m.group(1))) {
        days = -days;
      }
      tokens = hhmmss.substring(0, m.start()).split(":");
    } else {
      tokens = hhmmss.split(":");
    }

    if (tokens.length > 3 || tokens.length < 1) {
      throw new IllegalStateException(
        "Parse error, not a valid time (HH|HH:MM|HH:MM:SS): '" + hhmmss + "'"
      );
    }
    int hh = Integer.parseInt(tokens[0]);
    if (hh > 23) {
      days += hh / 24;
      hh = hh % 24;
    }
    int mm = tokens.length > 1 ? Integer.parseInt(tokens[1]) : 0;
    int ss = tokens.length == 3 ? Integer.parseInt(tokens[2]) : 0;
    int seconds = LocalTime.of(hh, mm, ss).toSecondOfDay() + days * ONE_DAY_SECONDS;
    return negative ? -seconds : seconds;
  }

  /**
   * Same as {@link #time(String)}, but returns default value if input is {@code null}, an empty
   * string or only contains whitespace.
   */
  public static int time(String hhmmss, int defaultValue) {
    if (hhmmss == null || hhmmss.isBlank()) {
      return defaultValue;
    }
    return time(hhmmss.trim());
  }

  /**
   * Parse a string of times like "00:20 01:20 05:57:30" into an array of local times. This can be
   * very helpful when specifying a schedule using a sting instead of using a int array with seconds
   * past midnight.
   */
  public static int[] times(String input) {
    return Arrays.stream(input.split("[ ,;]+")).mapToInt(TimeUtils::time).toArray();
  }

  /** Format string on format [H]H:MM[:SS]. Examples: 0:00, 8:31:11, 9:31 and 23:59:59.  */
  public static String timeToStrCompact(int time) {
    return RelativeTime.ofSeconds(time).toCompactStr();
  }

  public static String durationToStrCompact(Duration duration) {
    return timeToStrCompact((int) duration.toSeconds());
  }

  /** Format string on format [H]H:MM[:SS]. Examples: 0:00, 8:31:11, 9:31 and 23:59:59. */
  public static String timeToStrCompact(int time, int notSetValue) {
    return timeToStrCompact(time, notSetValue, "");
  }

  /** Format string on format [H]H:MM[:SS]. Examples: 0:00, 8:31:11, 9:31 and 23:59:59.  */
  public static String timeToStrCompact(int time, int notSetValue, String notSetText) {
    return time == notSetValue ? notSetText : RelativeTime.ofSeconds(time).toCompactStr();
  }

  /** Format string on format [H]H:MM[:SS]. Examples: 0:00, 8:31:11, 9:31 and 23:59:59.  */
  public static String timeToStrCompact(ZonedDateTime time) {
    return time == null ? "" : RelativeTime.from(time).toCompactStr();
  }

  /** Format string on format HH:MM:SS */
  public static String timeToStrLong(int time) {
    return RelativeTime.ofSeconds(time).toLongStr();
  }

  /** Format string on format HH:MM:SS */
  public static String timeToStrLong(int time, int notSetValue) {
    return time == notSetValue ? "" : RelativeTime.ofSeconds(time).toLongStr();
  }

  /** Format string on format HH:MM:SS */
  public static String timeToStrLong(ZonedDateTime time) {
    return RelativeTime.from(time).toLongStr();
  }

  public static String timeToStrLong(LocalTime time) {
    return time.toString();
  }

  /**
   * This method take a date, a time in seconds and a zoneId and create a {@link ZonedDateTime}.
   * <p>
   * This method follow the GTFS specification for time: "The time is measured from 'noon minus 12h'
   * of the service day (effectively midnight except for days on which daylight savings time changes
   * occur." See https://developers.google.com/transit/gtfs/reference#field_types
   * <p>
   *
   * @param date    the "service" date
   * @param seconds number of seconds since noon minus 12 hours (midnight).
   */
  public static ZonedDateTime zonedDateTime(LocalDate date, int seconds, ZoneId zoneId) {
    return RelativeTime.ofSeconds(seconds).toZonedDateTime(date, zoneId);
  }

  /**
   * Convert system time in milliseconds to a sting:
   * <pre>
   * -1100 -> -1.1s
   *     0 -> 0s
   *  1000 -> 1s
   *  1001 -> 1.001s
   *  1010 -> 1.01s
   *  1100 -> 1.1s
   * 23456 -> 23.456s
   * </pre>
   */
  public static String msToString(long milliseconds) {
    long seconds = milliseconds / 1000L;
    int decimals = Math.abs((int) (milliseconds % 1000));
    if (decimals == 0) {
      return seconds + "s";
    }
    if (decimals % 10 == 0) {
      decimals = decimals / 10;
      if (decimals % 10 == 0) {
        decimals = decimals / 10;
        return seconds + "." + decimals + "s";
      }
      return seconds + "." + String.format("%02d", decimals) + "s";
    }
    return seconds + "." + String.format("%03d", decimals) + "s";
  }

  /**
   * Wait (compute) until the given {@code waitMs} is past. The returned long is a very random
   * number. If this method is called twice a grace period of 5 times the wait-time is set. All
   * calls within the grace period will return immediately. This ensures only ONE wait is applied
   * to a given client request. Wait a bit, then make another request, and you will enter the
   * busy-wait again.
   * <p>
   * This method does a "busy" wait - it is not affected by a thread interrupt like
   * {@link Thread#sleep(long)}; Hence do not interfere with timeout logic which uses the interrupt
   * flag.
   * <p>
   * THIS CODE IS NOT MEANT FOR PRODUCTION!
   */
  @SuppressWarnings("unused")
  public static long busyWaitOnce(int waitMs) {
    long time = System.currentTimeMillis();
    if (time < BUSY_WAIT_GRACE_PERIOD_TIMEOUT.get()) {
      return 0;
    }
    BUSY_WAIT_GRACE_PERIOD_TIMEOUT.set(time + 5L * waitMs);

    return busyWait(waitMs);
  }

  /**
   * Wait (compute) until the given {@code waitMs} is past. The returned long is a very random
   * number.
   * <p>
   * This method does a "busy" wait - it is not affected by a thread interrupt like
   * {@link Thread#sleep(long)}; Hence do not interfere with timeout logic which uses the interrupt
   * flag.
   * <p>
   * THIS CODE IS NOT MEANT FOR PRODUCTION!
   */
  @SuppressWarnings("unused")
  public static long busyWait(int waitMs) {
    long waitUntil = System.currentTimeMillis() + waitMs;

    Random rnd = new SecureRandom();
    long value = rnd.nextLong();

    // Print wait, this method is for debugging only
    System.err.printf(Locale.ROOT, "BUSY-WAIT (%.1fs)%n", (waitMs / 1000.0));

    while (System.currentTimeMillis() < waitUntil) {
      value |= rnd.nextLong();
    }
    return value;
  }

  /**
   * Calculate the relative time in seconds with the given {@code transitSearchTimeZero} as the
   * base. There is no restriction on the returned time, it can be in the past(negative) and
   * many days ahead of the base. This method can be used to translate an API instance of time
   * into the OTP internal transit model time, when the search zero-point-in-time is known.
   */
  public static int toTransitTimeSeconds(ZonedDateTime transitSearchTimeZero, Instant time) {
    return (int) ChronoUnit.SECONDS.between(transitSearchTimeZero.toInstant(), time);
  }
}
