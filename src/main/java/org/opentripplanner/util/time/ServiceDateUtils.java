package org.opentripplanner.util.time;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;

public class ServiceDateUtils {

  public static ZonedDateTime asStartOfService(ZonedDateTime date) {
    return date.truncatedTo(ChronoUnit.HOURS).withHour(12).minusHours(12);
  }

  public static ZonedDateTime asStartOfService(Instant time, ZoneId zoneId) {
    LocalDate date = LocalDate.ofInstant(time, zoneId);
    return ServiceDateUtils.asStartOfService(date, zoneId);
  }

  public static ZonedDateTime asStartOfService(LocalDate localDate, ZoneId zoneId) {
    return ZonedDateTime.of(localDate, LocalTime.NOON, zoneId).minusHours(12);
  }

  /**
   * Create a ZonedDateTime based on the service date, time zone and seconds-offset. This
   * method add the offset seconds to the service date start time, which is defined to be NOON - 12
   * hours. This is midnight for most days, except days where the time is adjusted for daylight
   * saving time.
   */
  public static ZonedDateTime toZonedDateTime(
    LocalDate localDate,
    ZoneId zoneId,
    int secondsOffset
  ) {
    return asStartOfService(localDate, zoneId).plusSeconds(secondsOffset);
  }

  public static int secondsSinceStartOfTime(ZonedDateTime timeZero, LocalDate localDate) {
    ZonedDateTime startOfDay = asStartOfService(localDate, timeZero.getZone());
    return (int) Duration.between(timeZero, startOfDay).getSeconds();
  }

  public static int secondsSinceStartOfTime(ZonedDateTime startOfTime, Instant instant) {
    return (int) Duration.between(startOfTime.toInstant(), instant).getSeconds();
  }

  public static LocalDateTime asDateTime(LocalDate localDate, int secondsSinceStartOfDay) {
    // In OTP LocalDate is sometimes used to represent ServiceDate. This calculation is
    // "safe" because calculations on LocalDate ignore TimeZone adjustments, just like the
    // ServiceDate. So, in this case it is not necessary to: 'NOON - 12 hours + secondsSinceStartOfDay'
    return localDate.atStartOfDay().plusSeconds(secondsSinceStartOfDay);
  }

  public static int secondsSinceStartOfService(
    ZonedDateTime operatingDayDate,
    ZonedDateTime dateTime,
    ZoneId zoneId
  ) {
    ZonedDateTime startOfService = asStartOfService(
      operatingDayDate.withZoneSameInstant(zoneId).toLocalDate(),
      zoneId
    );
    return (int) Duration.between(startOfService, dateTime).toSeconds();
  }

  public static int secondsSinceStartOfService(
    ZonedDateTime startOfService,
    ZonedDateTime dateTime
  ) {
    return (int) Duration.between(startOfService, dateTime).toSeconds();
  }

  /**
   * The service date is either the minimum or maximum allowed value. In practice this means
   * unbounded.
   */
  public static boolean isMinMax(LocalDate date) {
    return LocalDate.MIN.equals(date) || LocalDate.MAX.equals(date);
  }
}
