package org.opentripplanner._support.time;

import java.time.LocalDate;
import java.time.ZonedDateTime;

public class DateTimes {

  public static final ZonedDateTime ANY_ZONED_DATE_TIME_1 = ZonedDateTime.parse(
    "2025-01-14T14:01:21+01:00"
  );
  public static final ZonedDateTime ANY_ZONED_DATE_TIME_2 = ANY_ZONED_DATE_TIME_1.plusHours(3);
  public static final LocalDate ANY_LOCAL_DATE = LocalDate.of(2025, 1, 15);
}
