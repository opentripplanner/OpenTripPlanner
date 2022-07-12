package org.opentripplanner.util;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;

public class TestUtils {

  public static final int JANUARY = 0;
  public static final int FEBRUARY = 1;
  public static final int MARCH = 2;
  public static final int APRIL = 3;
  public static final int MAY = 4;
  public static final int JUNE = 5;
  public static final int JULY = 6;
  public static final int AUGUST = 7;
  public static final int SEPTEMBER = 8;
  public static final int OCTOBER = 9;
  public static final int NOVEMBER = 10;
  public static final int DECEMBER = 11;

  public static Instant dateInstant(
    String zoneId,
    int year,
    int month,
    int day,
    int hour,
    int minute,
    int second
  ) {
    return ZonedDateTime
      .of(year, month, day, hour, minute, second, 0, ZoneId.of(zoneId))
      .toInstant();
  }

  public static long dateInSeconds(
    String timeZoneId,
    int year,
    int month,
    int day,
    int hour,
    int minute,
    int second
  ) {
    ZonedDateTime time = ZonedDateTime.of(
      year,
      month + 1,
      day,
      hour,
      minute,
      second,
      0,
      ZoneId.of(timeZoneId)
    );
    return time.toEpochSecond();
  }
}
