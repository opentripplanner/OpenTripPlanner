package org.opentripplanner.util.time;

import org.opentripplanner.model.calendar.ServiceDate;

import java.time.Duration;
import java.time.LocalTime;


/**
 * This class extend the Java {@link LocalTime} and with the ability
 * to span multiple days and be negative.
 * <p>
 * The class is used to track time relative to a {@link ServiceDate}.
 * <p>
 * The RelativeTime can also be used relative to some other time, in witch case it is similar
 * to the JAva {@link Duration}.
 * <p>
 * The primary usage of this class is to convert "number of seconds" into a string
 * in the given context. Here is some examples:
 * <pre>
 *   Service date time:
 *   23:59:59     // One second to midnight
 *   2:00+1d      // 26 hours (1d2h) past midnight of service date
 *   12:05-3d     // 5 past noon on service-date minus 3 days
 *   -11:00+1d    // 1 day and 11 hours before midnight service-date
 *   2d4h12s      // 2 days 4 hours and 12 seconds (relative to time in context)
 *   -3m          // 3 minutes before  (relative to time in context)
 * </pre>
 *
 */
public class DurationUtils {

  private DurationUtils() { }


  /**
   * Convert a duration in seconds to a readable string. This a follows
   * the ISO-8601 notation for most parts, but make it a bit more readable:
   * {@code P2DT2H12M40S => 2d2h12m40s}. For negative durations
   * {@code -P2dT3s => -2d3s, not -2d-3s or -(2d3s)} is used.
   */
  public static String durationToStr(int timeSeconds) {
    StringBuilder buf = new StringBuilder();
    if(timeSeconds < 0) { buf.append("-"); }
    int time = Math.abs(timeSeconds);
    int sec = time % 60;
    time = time / 60;
    int min = time % 60;
    time = time / 60;
    int hour = time % 24;
    int day = time / 24;

    if(day != 0) { buf.append(day).append('d'); }
    if(hour != 0) { buf.append(hour).append('h'); }
    if(min != 0) { buf.append(min).append('m'); }
    if(sec != 0) { buf.append(sec).append('s'); }

    return buf.length() == 0 ? "0s" : buf.toString();
  }

  public static String durationToStr(Duration duration) {
    return durationToStr((int)duration.toSeconds());
  }

  public static String durationToStr(int timeSeconds, int notSetValue) {
    return timeSeconds == notSetValue ? "" : durationToStr(timeSeconds);
  }

  /**
   * Parse a sting on format {@code nHnMn.nS}.
   * Same as @see Duration#parse(CharSequence) with argument: {@code "PT" + duration}
   */
  public static int duration(String duration) {
    Duration d = Duration.parse("PT" + duration);
    return (int)d.toSeconds();
  }

  public static String msToSecondsStr(long timeMs) {
    if(timeMs == 0) { return "0 seconds"; }
    if(timeMs == 1000) { return "1 second"; }
    if(timeMs < 100) { return String.format ("%.3f seconds",  timeMs/1000.0); }
    if(timeMs < 995) { return String.format ("%.2f seconds",  timeMs/1000.0); }
    if(timeMs < 9950) { return String.format ("%.1f seconds",  timeMs/1000.0); }
    else { return String.format ("%.0f seconds",  timeMs/1000.0); }
  }
}
