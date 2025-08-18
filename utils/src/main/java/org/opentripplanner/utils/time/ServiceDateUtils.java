package org.opentripplanner.utils.time;

import java.text.ParseException;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utilities related to handling of service dates. In GTFS, service dates are defined to begin 12
 * hours before noon. This requires us to do some calculations when not using timezone-aware ways to
 * do calculations on time, as days with DST-changes make the day start either before or after
 * midnight.
 */
public class ServiceDateUtils {

  private static final String MAX_TEXT = "MAX";
  private static final String MIN_TEXT = "MIN";
  private static final Pattern PATTERN = Pattern.compile("^(\\d{4})-?(\\d{2})-?(\\d{2})$");
  private static final DateTimeFormatter COMPACT_FORMATTER = DateTimeFormatter.ofPattern(
    "uuuuMMdd"
  );

  /**
   * Calculate the start of the service day for the given {@link Instant} at the given
   * {@link ZoneId}
   */
  public static ZonedDateTime asStartOfService(Instant time, ZoneId zoneId) {
    LocalDate date = LocalDate.ofInstant(time, zoneId);
    return ServiceDateUtils.asStartOfService(date, zoneId);
  }

  /**
   * Calculate the start of the service day for the given {@link LocalDate} at the given
   * {@link ZoneId}
   */
  public static ZonedDateTime asStartOfService(LocalDate localDate, ZoneId zoneId) {
    return ZonedDateTime.of(localDate, LocalTime.NOON, zoneId).minusHours(12);
  }

  /**
   * Calculate the service day from start of the service day. On days with daylight saving
   * time adjustments this may not be the same as {@code startOfService.toLocalDate()}.
   * Adding 12 hours is necessary.
   */
  public static LocalDate asServiceDay(ZonedDateTime startOfService) {
    return startOfService.plusHours(12).toLocalDate();
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

  public static OffsetDateTime toOffsetDateTime(
    LocalDate localDate,
    ZoneId zoneId,
    int secondsOffset
  ) {
    return toZonedDateTime(localDate, zoneId, secondsOffset).toOffsetDateTime();
  }

  public static int secondsSinceStartOfTime(ZonedDateTime timeZero, LocalDate localDate) {
    ZonedDateTime startOfDay = asStartOfService(localDate, timeZero.getZone());
    return (int) Duration.between(timeZero, startOfDay).getSeconds();
  }

  public static int secondsSinceStartOfTime(ZonedDateTime startOfTime, Instant instant) {
    return (int) Duration.between(startOfTime.toInstant(), instant).getSeconds();
  }

  public static LocalDateTime asDateTime(LocalDate localDate, int secondsSinceStartOfDay) {
    // This calculation is "safe" because calculations on LocalDate ignore TimeZone adjustments.
    // So, in this case it is not necessary to: 'NOON - 12 hours + secondsSinceStartOfDay'
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
   * Parse given input string in the "YYYYMMDD" or "YYYY-MM-DD" format.
   *
   * @throws ParseException on parse error
   */
  public static LocalDate parseString(String value) throws ParseException {
    Matcher matcher = PATTERN.matcher(value);

    if (!matcher.matches()) {
      throw new ParseException("error parsing date: " + value, 0);
    }

    int year = Integer.parseInt(matcher.group(1));
    int month = Integer.parseInt(matcher.group(2));
    int day = Integer.parseInt(matcher.group(3));
    return LocalDate.of(year, month, day);
  }

  public static Optional<LocalDate> parseStringToOptional(String value) {
    try {
      return Optional.of(parseString(value));
    } catch (ParseException e) {
      return Optional.empty();
    }
  }

  /**
   * The service date is either the minimum or maximum allowed value. In practice this means
   * unbounded.
   */
  public static boolean isMinMax(LocalDate date) {
    return LocalDate.MIN.equals(date) || LocalDate.MAX.equals(date);
  }

  public static LocalDate max(LocalDate a, LocalDate b) {
    return a.isAfter(b) ? a : b;
  }

  public static LocalDate min(LocalDate a, LocalDate b) {
    return a.isBefore(b) ? a : b;
  }

  /**
   * @return a string in "YYYYMMDD" format
   */
  public static String asCompactString(LocalDate date) {
    return date.format(COMPACT_FORMATTER);
  }

  public static String toString(LocalDate date) {
    if (LocalDate.MAX.equals(date)) {
      return MAX_TEXT;
    }
    if (LocalDate.MIN.equals(date)) {
      return MIN_TEXT;
    }
    return date.toString();
  }
}
