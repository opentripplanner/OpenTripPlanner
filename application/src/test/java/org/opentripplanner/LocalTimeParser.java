package org.opentripplanner;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import org.opentripplanner.utils.time.DateUtils;
import org.opentripplanner.utils.time.TimeUtils;

/**
 * A helper class used in tests for parsing local time strings into absolute times given a date and
 * a timezone.
 */
public class LocalTimeParser {

  private final ZoneId zone;
  private final LocalDate defaultDate;

  public LocalTimeParser(ZoneId zone, LocalDate defaultDate) {
    this.zone = zone;
    this.defaultDate = defaultDate;
  }

  /**
   * Create a zoned datetime from a time string using the default date and timezone
   *
   * @throws IllegalArgumentException if we can't parse the string as a time.
   */
  public ZonedDateTime zonedDateTime(String timeString) {
    var time = DateUtils.parseTime(timeString);
    if (time == null) {
      throw new IllegalArgumentException("Invalid time format");
    }
    return defaultDate.atTime(time).atZone(zone);
  }

  /**
   * Takes local time on different formats like "12:34:00" or "12:34" and returns the corresponding
   * instant at the given date on the specific timezone.
   */
  public Instant instant(String timeString) {
    var localTime = LocalTime.ofSecondOfDay(TimeUtils.time(timeString));
    return localTime.atDate(defaultDate).atZone(zone).toInstant();
  }
}
