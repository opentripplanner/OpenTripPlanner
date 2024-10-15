package org.opentripplanner;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import org.opentripplanner.framework.time.DateUtils;

public class DateTimeHelper {

  private final ZoneId zone;
  private final LocalDate defaultDate;

  public DateTimeHelper(ZoneId zone, LocalDate defaultDate) {
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
}
