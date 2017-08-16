/**
 * Copyright (C) 2011 Brian Ferris <bdferris@onebusaway.org>
 * Copyright (C) 2011 Google, Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.onebusaway.gtfs.model.calendar;

import java.io.Serializable;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.onebusaway.gtfs.model.ServiceCalendar;
import org.onebusaway.gtfs.model.ServiceCalendarDate;
import org.onebusaway.gtfs.model.StopTime;

/**
 * A general representation of a year-month-day tuple not tied to any locale and
 * used by the GTFS entities {@link ServiceCalendar} and
 * {@link ServiceCalendarDate} to represent service date ranges. A service date
 * is a particular date when a particular GTFS service id is active.
 * 
 * @author bdferris
 * 
 */
public class ServiceDate implements Serializable, Comparable<ServiceDate> {

  private static final long serialVersionUID = 1L;

  private static final Pattern _pattern = Pattern.compile("^(\\d{4})(\\d{2})(\\d{2})$");

  private static final NumberFormat _yearFormat = new DecimalFormat("0000");

  private static final NumberFormat _monthAndDayFormat = new DecimalFormat("00");

  private static final TimeZone _utcTimeZone = TimeZone.getTimeZone("UTC");

  private final int year;

  private final int month;

  private final int day;

  /**
   * Construct a new ServiceDate by specifying the numeric year, month, and day
   * 
   * @param year - numeric year (ex. 2010)
   * @param month - numeric month of the year, where Jan = 1, Feb = 2, etc
   * @param day - numeric day of month
   */
  public ServiceDate(int year, int month, int day) {
    this.year = year;
    this.month = month;
    this.day = day;
  }

  public ServiceDate(ServiceDate o) {
    this(o.year, o.month, o.day);
  }

  public ServiceDate(Calendar calendar) {
    this(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH) + 1,
        calendar.get(Calendar.DAY_OF_MONTH));
  }

  /**
   * Construct a ServiceDate from the specified {@link Date} object, using the
   * default {@link TimeZone} object for the current VM to localize the date
   * 
   * @param date
   */
  public ServiceDate(Date date) {
    this(getCalendarForDate(date));
  }

  public ServiceDate() {
    this(new Date());
  }

  /**
   * Parse a service date from a string in "YYYYMMDD" format.
   * 
   * @param value a string of the form "YYYYMMDD"
   * @return a new ServiceDate object
   * @throws ParseException on parse error
   */
  public static ServiceDate parseString(String value) throws ParseException {

    Matcher matcher = _pattern.matcher(value);

    if (!matcher.matches())
      throw new ParseException("error parsing date: " + value, 0);

    int year = Integer.parseInt(matcher.group(1));
    int month = Integer.parseInt(matcher.group(2));
    int day = Integer.parseInt(matcher.group(3));
    return new ServiceDate(year, month, day);
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
   * @return calls {@link #getAsDate(TimeZone)} with the default timezone for
   *         this VM
   */
  public Date getAsDate() {
    return getAsDate(TimeZone.getDefault());
  }

  /**
   * Constructs a {@link Calendar} object such that the Calendar will be at
   * "midnight" (12:00am) at the start of the day specified by this service date
   * and the target timezone. Note that we take the GTFS convention of
   * calculating midnight by setting the target date to noon (12:00pm) for the
   * service date and timezone specified and then subtracting twelve hours.
   * Normally that would be equivalent to midnight, except on Daylight Saving
   * Time days, in which case it can be an hour ahead or behind. This behavior
   * ensures correct calculation of {@link StopTime} arrival and departure time
   * when the second offset is added to the localized service date.
   * 
   * @param timeZone the target timezone to localize the service date to
   * @return a localized date at "midnight" at the start of this service date in
   *         the specified timezone
   */
  public Calendar getAsCalendar(TimeZone timeZone) {
    Calendar c = Calendar.getInstance();
    c.setTimeZone(timeZone);
    c.set(Calendar.YEAR, year);
    c.set(Calendar.MONTH, month - 1);
    c.set(Calendar.DAY_OF_MONTH, day);

    moveCalendarToServiceDate(c);

    return c;
  }

  /**
   * See {@link #getAsCalendar(TimeZone)} for more details.
   * 
   * @param timeZone the target timezone to localize the service date to
   * @return a localized date at "midnight" at the start of this service date in
   *         the specified timezone
   */
  public Date getAsDate(TimeZone timeZone) {
    Calendar c = getAsCalendar(timeZone);
    return c.getTime();
  }

  /**
   * @return a string in "YYYYMMDD" format
   */
  public String getAsString() {
    String year = _yearFormat.format(this.year);
    String month = _monthAndDayFormat.format(this.month);
    String day = _monthAndDayFormat.format(this.day);
    return year + month + day;
  }

  /**
   * 
   * @return the service date following the current service date
   */
  public ServiceDate next() {
    return shift(1);
  }

  /**
   * 
   * @return the service date preceding the current service date
   */
  public ServiceDate previous() {
    return shift(-1);
  }

  /**
   * 
   * @param numberOfDays
   * @return the service date following the current service date by the
   *         specified number of days, or preceding if a negative number of days
   *         is specified
   */
  public ServiceDate shift(int numberOfDays) {
    Calendar c = getAsCalendar(_utcTimeZone);
    c.add(Calendar.DAY_OF_YEAR, numberOfDays);
    return new ServiceDate(c);
  }

  /**
   * @param serviceDate
   * @return the number of days between this service date and the specified
   *         argument service date
   */
  public long difference(ServiceDate serviceDate) {
    return (serviceDate.getAsDate(_utcTimeZone).getTime() - getAsDate(
        _utcTimeZone).getTime())
        / (24 * 60 * 60 * 1000);
  }

  @Override
  public int compareTo(ServiceDate o) {
    int c = this.year - o.year;
    if (c == 0)
      c = this.month - o.month;
    if (c == 0)
      c = this.day - o.day;
    return c;
  }

  @Override
  public String toString() {
    return "ServiceIdDate(" + year + "-" + month + "-" + day + ")";
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + day;
    result = prime * result + month;
    result = prime * result + year;
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (getClass() != obj.getClass())
      return false;
    ServiceDate other = (ServiceDate) obj;
    if (day != other.day)
      return false;
    if (month != other.month)
      return false;
    if (year != other.year)
      return false;
    return true;
  }

  /**
   * Adjust the supplied {@link Calendar} object such that the calendar will be
   * at "midnight" (12:00am) at the start of the day specified by the current
   * calendar date and locale. Note that we take the GTFS convention of
   * calculating midnight by setting the target date to noon (12:00pm) for the
   * service date and timezone specified and then subtracting twelve hours.
   * Normally that would be equivalent to midnight, except on Daylight Saving
   * Time days, in which case it can be an hour ahead or behind. This behavior
   * ensures correct calculation of {@link StopTime} arrival and departure time
   * when the second offset is added to the localized service date.
   * 
   * @param c the target calendar, already to some time on the target date
   */
  public static void moveCalendarToServiceDate(Calendar c) {
    // Initial set time to noon
    c.set(Calendar.HOUR_OF_DAY, 12);
    c.set(Calendar.MINUTE, 0);
    c.set(Calendar.SECOND, 0);
    c.set(Calendar.MILLISECOND, 0);

    // Subtract 12 hours. Usually takes you to midnight, except on DST days
    c.add(Calendar.HOUR_OF_DAY, -12);
  }

  /****
   * Private Methods
   ****/

  private static final Calendar getCalendarForDate(Date date) {
    Calendar c = Calendar.getInstance();
    c.setTime(date);
    return c;
  }

}
