package org.opentripplanner.utils.time;

import static java.util.Locale.ROOT;

import java.time.Duration;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * This class extend the Java {@link Duration} with utility functionality to parse and convert
 * integer and text to a {@link Duration}. This class also contains methods to validate durations.
 * <p>
 * OTP make have use of the Duration in a lenient ISO-8601 duration format. For example:
 * <pre>
 *   1s          // one second
 *   15m         // 15 minutes
 *   2h          // 2 hours
 *   2d          // 2 days
 *   2d3h12m40s  // 2 days, 3 hours, 12 minutes and 40 seconds
 *   -3m30s      // 3.5 minutes before  (relative to time in context)
 * </pre>
 */
public class DurationUtils {

  private static final Pattern DECIMAL_NUMBER_PATTERN = Pattern.compile("[-+]?\\d+");

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
    return duration == null ? "" : durationToStr((int) duration.toSeconds());
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
   * This is used to parse a string which may be a number {@code NNNN}(number of seconds) or a
   * duration with format {@code NhNmNs}, where {@code N} is a decimal number and
   * {@code h} is hours, {@code m} minutes and {@code s} seconds.
   * <p>
   * This method
   */
  public static Optional<Duration> parseSecondsOrDuration(String duration) {
    if (duration == null || duration.isBlank()) {
      return Optional.empty();
    }
    var s = duration.trim();

    return Optional.of(
      DECIMAL_NUMBER_PATTERN.matcher(s).matches()
        ? Duration.ofSeconds(Integer.parseInt(s))
        : duration(s)
    );
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
    return Arrays.stream(durations.split("[,;\\s]+"))
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

  public static Duration requireNonNegative(Duration value) {
    Objects.requireNonNull(value);
    if (value.isNegative()) {
      throw new IllegalArgumentException("Duration can't be negative: " + value);
    }
    return value;
  }

  /**
   * Checks that duration is positive and less than the given {@code maxLimit} (exclusive).
   *
   * @param subject used to identify name of the problematic value when throwing an exception.
   */
  public static Duration requireNonNegative(Duration value, Duration maxLimit, String subject) {
    Objects.requireNonNull(value);
    if (value.isNegative()) {
      throw new IllegalArgumentException(
        "Duration %s can't be negative: %s".formatted(subject, value)
      );
    }
    if (value.compareTo(maxLimit) >= 0) {
      throw new IllegalArgumentException(
        "Duration %s can't be longer or equals too %s: %s".formatted(
            subject,
            durationToStr(maxLimit),
            value
          )
      );
    }
    return value;
  }

  /**
   * Checks that duration is not negative and not over 2 days.
   *
   * @param subject used to identify name of the problematic value when throwing an exception.
   */
  public static Duration requireNonNegativeMax2days(Duration value, String subject) {
    return requireNonNegative(value, Duration.ofDays(2), subject);
  }

  /**
   * Checks that duration is not negative and not over 2 hours.
   *
   * @param subject used to identify name of the problematic value when throwing an exception.
   */
  public static Duration requireNonNegativeMax2hours(Duration value, String subject) {
    return requireNonNegative(value, Duration.ofHours(2), subject);
  }

  /**
   * Checks that duration is not negative and not over 30 minutes.
   *
   * @param subject used to identify name of the problematic value when throwing an exception.
   */
  public static Duration requireNonNegativeMax30minutes(Duration value, String subject) {
    return requireNonNegative(value, Duration.ofMinutes(30), subject);
  }

  /**
   * Convert duration to an int with unit milliseconds.
   */
  public static int toIntMilliseconds(Duration timeout, int defaultValue) {
    return timeout == null ? defaultValue : (int) timeout.toMillis();
  }

  private static String msToSecondsStr(String formatSeconds, double timeMs) {
    return String.format(ROOT, formatSeconds, timeMs / 1000.0) + " seconds";
  }

  /**
   * Formats a duration and if it's a negative amount, it places the minus before the "P" rather
   * than in the middle of the value.
   * <p>
   * Background: There are multiple ways to express -1.5 hours: "PT-1H-30M" and "-PT1H30M".
   * <p>
   * The first version is what you get when calling toString() but it's quite confusing. Therefore,
   * this method makes sure that you get the second form "-PT1H30M".
   */
  public static String formatDurationWithLeadingMinus(Duration duration) {
    if (duration.isNegative()) {
      var positive = duration.abs().toString();
      return "-" + positive;
    } else {
      return duration.toString();
    }
  }
}
