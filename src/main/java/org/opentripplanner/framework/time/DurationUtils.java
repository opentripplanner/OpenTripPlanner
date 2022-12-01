package org.opentripplanner.framework.time;

import static java.util.Locale.ROOT;

import java.time.Duration;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * This class extend the Java {@link LocalTime} and with the ability to span multiple days and be
 * negative.
 * <p>
 * The class is used to track time relative to a {@link java.time.LocalDate}.
 * <p>
 * The RelativeTime can also be used relative to some other time, in which case it is similar to the
 * JAva {@link Duration}.
 * <p>
 * The primary usage of this class is to convert "number of seconds" into a string in the given
 * context. Here is some examples:
 * <pre>
 *   Service date time:
 *   23:59:59     // One second to midnight
 *   2:00+1d      // 26 hours (1d2h) past midnight of service date
 *   12:05-3d     // 5 past noon on service-date minus 3 days
 *   -11:00+1d    // 1 day and 11 hours before midnight service-date
 *   2d4h12s      // 2 days 4 hours and 12 seconds (relative to time in context)
 *   -3m          // 3 minutes before  (relative to time in context)
 * </pre>
 */
public class DurationUtils {

  private DurationUtils() {}

  /**
   * Convert a duration in seconds to a readable string. This a follows the ISO-8601 notation for
   * most parts, but make it a bit more readable: {@code P2DT2H12M40S => 2d2h12m40s}. For negative
   * durations {@code -P2dT3s => -2d3s, not -2d-3s or -(2d3s)} is used.
   */
  public static String durationToStr(int timeSeconds) {
    StringBuilder buf = new StringBuilder();
    if (timeSeconds < 0) {
      buf.append("-");
    }
    int time = Math.abs(timeSeconds);
    int sec = time % 60;
    time = time / 60;
    int min = time % 60;
    time = time / 60;
    int hour = time % 24;
    int day = time / 24;

    if (day != 0) {
      buf.append(day).append('d');
    }
    if (hour != 0) {
      buf.append(hour).append('h');
    }
    if (min != 0) {
      buf.append(min).append('m');
    }
    if (sec != 0) {
      buf.append(sec).append('s');
    }

    return buf.length() == 0 ? "0s" : buf.toString();
  }

  public static String durationToStr(Duration duration) {
    return durationToStr((int) duration.toSeconds());
  }

  public static String durationToStr(int timeSeconds, int notSetValue) {
    return timeSeconds == notSetValue ? "" : durationToStr(timeSeconds);
  }

  /**
   * Parse a sting on format {@code nHnMn.nS} or {@link Duration#parse(CharSequence)}. The prefix
   * "PT" is added if not present before handed of the {@link Duration#parse(CharSequence)} method.
   *
   * @return the duration in seconds
   */
  public static int durationInSeconds(String duration) {
    Duration d = duration(duration);
    return (int) d.toSeconds();
  }

  /**
   * Parses the given duration string. If the string contains an integer number the string is
   * converted to a duration using the given unit. If not, the {@link #durations(String)} is used
   * and the given unit is ignored.
   */
  public static Duration duration(String duration, ChronoUnit unit) {
    if (duration.matches("-?\\d+")) {
      return Duration.of(Integer.parseInt(duration), unit);
    }
    return duration(duration);
  }

  /**
   * Same as {@link #durationInSeconds(String)}, but returns the {@link Duration}, not the {@code
   * int} seconds.
   */
  public static Duration duration(String duration) {
    var d = duration.toUpperCase();
    if (!(d.startsWith("P") || d.startsWith("-P"))) {
      int pos = d.indexOf('D') + 1;
      if (pos > 0) {
        var days = d.substring(0, pos);
        d = pos == d.length() ? "P" + days : "P" + days + "T" + d.substring(pos);
      } else {
        d = "PT" + d;
      }
    }
    try {
      return Duration.parse(d);
    } catch (DateTimeParseException e) {
      throw new DateTimeParseException(
        e.getMessage() + ": '" + duration + "'",
        duration,
        e.getErrorIndex()
      );
    }
  }

  /**
   * Parse a list of durations using white-space, comma or semicolon as a separator.
   * <p>
   * Example: {@code "2h 3m;5s"} will result in a list with 3 durations.
   */
  public static List<Duration> durations(String durations) {
    if (durations == null || durations.isBlank()) {
      return List.of();
    }
    return Arrays
      .stream(durations.split("[,;\\s]+"))
      .map(DurationUtils::duration)
      .collect(Collectors.toList());
  }

  public static String msToSecondsStr(long timeMs) {
    if (timeMs == 0) {
      return "0 seconds";
    }
    if (timeMs == 1000) {
      return "1 second";
    }
    if (timeMs < 100) {
      return msToSecondsStr("%.3f", timeMs);
    }
    if (timeMs < 995) {
      return msToSecondsStr("%.2f", timeMs);
    }
    if (timeMs < 9950) {
      return msToSecondsStr("%.1f", timeMs);
    } else {
      return msToSecondsStr("%.0f", timeMs);
    }
  }

  private static String msToSecondsStr(String formatSeconds, double timeMs) {
    return String.format(ROOT, formatSeconds, timeMs / 1000.0) + " seconds";
  }
}
