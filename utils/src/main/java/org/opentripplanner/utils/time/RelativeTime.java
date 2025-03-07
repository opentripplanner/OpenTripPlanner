package org.opentripplanner.utils.time;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

/**
 * This class extend the Java {@link LocalTime} and with the ability to span multiple days and be
 * negative.
 * <p>
 * The class is used to track time relative to a {@link LocalDate}.
 * <p>
 * The primary usage of this class is to convert "number of seconds" into a string in the given
 * context. Here is some examples:
 * <pre>
 *   Service date time:
 *   23:59:59     // One second to midnight
 *   2:00+1d      // 26 hours (1d2h) past midnight of service date
 *   12:05-3d     // 5 past noon on service-date minus 3 days
 *   11:00+1d     // 1 day and 11 hours after midnight service-date
 * </pre>
 */
class RelativeTime {

  private final int days;
  private final LocalTime time;

  private RelativeTime(int days, LocalTime time) {
    this.days = days;
    this.time = time;
  }

  private RelativeTime(int days, int hours, int minutes, int seconds) {
    this(days, LocalTime.of(hours, minutes, seconds));
  }

  /**
   * Convert to ZonedDateTime. Follow the GTFS spec for resolving the absolute time from a relative
   * time(11:00), date(2020-03-12) and time-zone. The time is relative to "noon - 12 hours", which
   * for most days are midnight, but in when time is adjusted for day-light-saving it is not.
   */
  public ZonedDateTime toZonedDateTime(LocalDate date, ZoneId zoneId) {
    return ServiceDateUtils.asStartOfService(date, zoneId)
      .plusDays(days)
      .plusSeconds(time.toSecondOfDay());
  }

  /**
   * Create time based on given number of seconds past midnight.
   *
   * @param secondsPastMidnight can be negative.
   */
  static RelativeTime ofSeconds(int secondsPastMidnight) {
    boolean negative = secondsPastMidnight < 0;
    int days = secondsPastMidnight / TimeUtils.ONE_DAY_SECONDS;
    int secondsOfDay = secondsPastMidnight % TimeUtils.ONE_DAY_SECONDS;

    if (negative) {
      // The days and secondsOfDay are both negative numbers
      return new RelativeTime(days - 1, LocalTime.MIDNIGHT.plusSeconds(secondsOfDay));
    } else {
      return new RelativeTime(days, LocalTime.ofSecondOfDay(secondsOfDay));
    }
  }

  static RelativeTime from(ZonedDateTime time) {
    return new RelativeTime(0, time.getHour(), time.getMinute(), time.getSecond());
  }

  String toLongStr() {
    return appendDays(timeStrLong());
  }

  String toCompactStr() {
    return appendDays(timeStrCompact());
  }

  private String timeStrCompact() {
    return time.getSecond() == 0
      ? String.format("%d:%02d", time.getHour(), time.getMinute())
      : String.format("%d:%02d:%02d", time.getHour(), time.getMinute(), time.getSecond());
  }

  private String timeStrLong() {
    return String.format("%02d:%02d:%02d", time.getHour(), time.getMinute(), time.getSecond());
  }

  private String appendDays(String body) {
    return days == 0 ? body : body + (days < 0 ? "" : "+") + days + "d";
  }
}
