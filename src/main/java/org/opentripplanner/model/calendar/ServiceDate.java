/* This file is based on code copied from project OneBusAway, see the LICENSE file for further information. */
package org.opentripplanner.model.calendar;

import java.io.Serializable;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.ParseException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.opentripplanner.util.time.ServiceDateUtils;

/**
 * A general representation of a year-month-day triple not tied to any locale and used by the GTFS
 * entities {@link ServiceCalendar} and {@link ServiceCalendarDate} to represent service date
 * ranges.
 * <p/>
 * A service date is a particular date when a particular GTFS service id is active.
 * <p/>
 * This class is immutable. It is a ValueObject(DesignPattern).
 */
public final class ServiceDate implements Serializable, Comparable<ServiceDate> {

  private static final String MAX_TEXT = "MAX";
  private static final String MIN_TEXT = "MIN";

  private static final Pattern PATTERN = Pattern.compile("^(\\d{4})-?(\\d{2})-?(\\d{2})$");

  private static final NumberFormat YEAR_FORMAT = new DecimalFormat("0000");

  private static final NumberFormat MONTH_AND_DAY_FORMAT = new DecimalFormat("00");

  /**
   * The smallest possible ServiceDate allowed. Dates before 1 . JAN year 0 is not allowed.
   */
  public static final ServiceDate MIN_DATE = new ServiceDate(0, 1, 1);

  /**
   * The greatest possible ServiceDate allowed. Dates after 31 . DEC year 9999 is not allowed.
   */
  public static final ServiceDate MAX_DATE = new ServiceDate(9999, 12, 31);

  private final int year;

  private final int month;

  private final int day;

  /**
   * A uniq increasing number for any valid day between 0000-01-01 and 9999-12-31. Holes in the
   * sequence is allowed to simplify the calculation. This is used for easy and fast caparison and
   * as a hash for this instant.
   * <p>
   * The value can safely be used for comparison, equals and hashCode.
   */
  private final int sequenceNumber;

  /**
   * Construct a new ServiceDate by specifying the numeric year, month, and day.
   * <p>
   * The date must be a valid date between year 1900-01-01 and 9999-12-31.
   *
   * @param year  - numeric year (ex. 2010)
   * @param month - numeric month of the year, where Jan = 1, Feb = 2, etc
   * @param day   - numeric day of month between 1 and 31.
   */
  public ServiceDate(int year, int month, int day) {
    // Preconditions
    verifyIsInRange(year, 0, 9999, "year");
    verifyIsInRange(month, 1, 12, "month");
    verifyIsInRange(day, 1, 31, "day");

    this.year = year;
    this.month = month;
    this.day = day;

    // The sequence number is constructed to be 'yyyymmdd' (a valid integer)
    this.sequenceNumber = 10_000 * year + 100 * month + day;
  }

  public ServiceDate(ZoneId timeZone) {
    this(LocalDate.now(timeZone));
  }

  public ServiceDate(LocalDate date) {
    this(date.getYear(), date.getMonthValue(), date.getDayOfMonth());
  }

  /**
   * Parse given input string in the "YYYYMMDD" or "YYYY-MM-DD" format.
   *
   * @throws ParseException on parse error
   */
  public static ServiceDate parseString(String value) throws ParseException {
    return new ServiceDate(ServiceDateUtils.parseString(value));
  }

  public static Optional<ServiceDate> parseStringToOptional(String value) {
    try {
      return Optional.of(parseString(value));
    } catch (ParseException e) {
      return Optional.empty();
    }
  }

  public int getYear() {
    return year;
  }

  public int getMonth() {
    return month;
  }

  public int getDay() {
    return day;
  }

  /**
   * Create a ZonedDateTime based on the current service date, time zone and seconds-offset. This
   * method add the offset seconds to the service date start time, which is defined to be NOON - 12
   * hours. This is midnight for most days, except days where the time is adjusted for daylight
   * saving time.
   */
  public ZonedDateTime toZonedDateTime(ZoneId zoneId, int secondsOffset) {
    return ServiceDateUtils.toZonedDateTime(toLocalDate(), zoneId, secondsOffset);
  }

  public ZonedDateTime getStartOfService(ZoneId zoneId) {
    return ServiceDateUtils.asStartOfService(toLocalDate(), zoneId);
  }

  /**
   * Add a given number of seconds to the service date and convert it to a new service date if it
   * the new time is on another date. The given time-zone is used to account for days which do not
   * have 24 hours (switching between summer and winter time).
   */
  public ServiceDate plusSeconds(ZoneId zoneId, int seconds) {
    return new ServiceDate(toZonedDateTime(zoneId, seconds).toLocalDate());
  }

  public LocalDate toLocalDate() {
    return LocalDate.of(year, month, day);
  }

  public String asISO8601() {
    return String.format("%d-%02d-%02d", year, month, day);
  }

  /**
   * @return the service date following the current service date
   */
  public ServiceDate next() {
    return shift(1);
  }

  /**
   * @return the service date preceding the current service date
   */
  public ServiceDate previous() {
    return shift(-1);
  }

  /**
   * @param numberOfDays number of days to shift current value, negative values are accepted.
   * @return the service date following the current service date by the specified number of days, or
   * preceding if a negative number of days is specified
   */
  public ServiceDate shift(int numberOfDays) {
    if (numberOfDays == 0) {
      return this;
    }
    return new ServiceDate(toLocalDate().plusDays(numberOfDays));
  }

  public boolean isBefore(ServiceDate other) {
    return sequenceNumber < other.sequenceNumber;
  }

  public boolean isBeforeOrEq(ServiceDate other) {
    return sequenceNumber <= other.sequenceNumber;
  }

  public boolean isAfter(ServiceDate other) {
    return sequenceNumber > other.sequenceNumber;
  }

  public boolean isAfterOrEq(ServiceDate other) {
    return sequenceNumber >= other.sequenceNumber;
  }

  @Override
  public int compareTo(ServiceDate o) {
    return sequenceNumber - o.sequenceNumber;
  }

  @Override
  public int hashCode() {
    return sequenceNumber;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    ServiceDate other = (ServiceDate) obj;
    return sequenceNumber == other.sequenceNumber;
  }

  @Override
  public String toString() {
    if (MAX_DATE.equals(this)) {
      return MAX_TEXT;
    }
    if (MIN_DATE.equals(this)) {
      return MIN_TEXT;
    }
    return asISO8601();
  }

  /* Private Methods */

  /** {@code min} and {@code max} are exclusive. */
  private static void verifyIsInRange(int v, int min, int max, String name) {
    if (v < min || v > max) {
      throw new IllegalArgumentException(
        "The ServiceDate " +
        name +
        " is not valid. The value " +
        v +
        " is not in range [" +
        min +
        ", " +
        max +
        "]."
      );
    }
  }
}
