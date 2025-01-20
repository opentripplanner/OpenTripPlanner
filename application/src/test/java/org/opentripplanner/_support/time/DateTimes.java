package org.opentripplanner._support.time;

import java.time.ZonedDateTime;

public class DateTimes {

  public static final ZonedDateTime ZONED_DATE_TIME_1 = ZonedDateTime.parse(
    "2025-01-14T22:01:21+01:00"
  );
  public static final ZonedDateTime ZONED_DATE_TIME_2 = ZONED_DATE_TIME_1.plusHours(3);
}
