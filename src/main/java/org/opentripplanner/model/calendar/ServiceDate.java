/* This file is based on code copied from project OneBusAway, see the LICENSE file for further information. */
package org.opentripplanner.model.calendar;

import java.io.Serializable;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.ParseException;
import java.time.LocalDate;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A general representation of a year-month-day triple not tied to any locale and
 * used by the GTFS entities {@link ServiceCalendar} and
 * {@link ServiceCalendarDate} to represent service date ranges.
 * <p/>
 * A service date is a particular date when a particular GTFS service id is active.
 * <p/>
 * This class is immutable. It is a ValueObject(DesignPattern).
 */
public final class ServiceDate implements Serializable, Comparable<ServiceDate> {

    private static final long serialVersionUID = 1L;

    private static final Pattern PATTERN = Pattern.compile("^(\\d{4})-?(\\d{2})-?(\\d{2})$");

    private static final NumberFormat YEAR_FORMAT = new DecimalFormat("0000");

    private static final NumberFormat MONTH_AND_DAY_FORMAT = new DecimalFormat("00");

    private static final TimeZone UTC_TIME_ZONE = TimeZone.getTimeZone("UTC");

    /**
     * The smallest possible ServiceDate allowed. Dates before 1 . JAN year 0 is not allowed.
     */
    public static final ServiceDate MIN_DATE = new ServiceDate(0, 1, 1);

    /**
     * The greatest possible ServiceDate allowed. Dates ater 31 . DEC year 9999 is not allowed.
     */
    public static final ServiceDate MAX_DATE = new ServiceDate(9999, 12, 31);


    private final int year;

    private final int month;

    private final int day;

    /**
     * A uniq increasing number for any valid day between 0000-01-01 and 9999-12-31.
     * Holes in the sequence is allowed to simplify the calculation. This is used for
     * easy and fast caparison and as a hash for this instant.
     *
     * The value can safely be used for comparison, equals and hashCode.
     */
    private final int sequenceNumber;


    /**
     * Construct a new ServiceDate by specifying the numeric year, month, and day.
     *
     * The date must be a valid date between year 1900-01-01 and 9999-12-31.
     *
     * @param year - numeric year (ex. 2010)
     * @param month - numeric month of the year, where Jan = 1, Feb = 2, etc
     * @param day - numeric day of month between 1 and 31.
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

    public ServiceDate(Calendar calendar) {
        this(
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH) + 1,
                calendar.get(Calendar.DAY_OF_MONTH)
        );
    }

    /**
     * Construct a ServiceDate from the specified {@link Date} object, using the
     * default {@link TimeZone} object for the current VM to localize the date
     */
    public ServiceDate(Date date) {
        this(getCalendarForDate(date));
    }

    public ServiceDate() {
        this(new Date());
    }

    public ServiceDate(LocalDate date) {
        this(date.getYear(), date.getMonthValue(), date.getDayOfMonth());
    }

    /**
     * Parse a service date from a string in "YYYYMMDD" format.
     *
     * @param value a string of the form "YYYYMMDD"
     * @return a new ServiceDate object
     * @throws ParseException on parse error
     */
    public static ServiceDate parseString(String value) throws ParseException {

        Matcher matcher = PATTERN.matcher(value);

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
     * ensures correct calculation of {@link org.opentripplanner.model.StopTime}
     * arrival and departure time when the second offset is added to the localized
     * service date.
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
    public String asCompactString() {
        String year = YEAR_FORMAT.format(this.year);
        String month = MONTH_AND_DAY_FORMAT.format(this.month);
        String day = MONTH_AND_DAY_FORMAT.format(this.day);
        return year + month + day;
    }

    public String asISO8601() {
        return String.format("%d-%02d-%02d", year, month, day);
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
        Calendar c = getAsCalendar(UTC_TIME_ZONE);
        c.add(Calendar.DAY_OF_YEAR, numberOfDays);
        return new ServiceDate(c);
    }

    /**
     * @param serviceDate
     * @return the number of days between this service date and the specified
     *         argument service date
     */
    public long difference(ServiceDate serviceDate) {
        return (serviceDate.getAsDate(UTC_TIME_ZONE).getTime() - getAsDate(UTC_TIME_ZONE).getTime())
                / (24 * 60 * 60 * 1000);
    }

    /**
     * The service date is either the minimum or maximum allowed value.
     * In practice this means unbounded.
     * */
    public boolean isMinMax() {
        return equals(MIN_DATE) || equals(MAX_DATE);
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

    public ServiceDate min(ServiceDate other) {
        return isBefore(other) ? this : other;
    }

    public ServiceDate max(ServiceDate other) {
        return isAfter(other) ? this : other;
    }

    @Override
    public int compareTo(ServiceDate o) {
        return sequenceNumber - o.sequenceNumber;
    }

    @Override
    public String toString() {
        if(MAX_DATE.equals(this)) { return "MAX"; }
        if(MIN_DATE.equals(this)) { return "MIN"; }
        return asISO8601();
    }

    @Override
    public int hashCode() {
        return sequenceNumber;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) { return true; }
        if (obj == null) { return false; }
        if (getClass() != obj.getClass()) { return false; }
        ServiceDate other = (ServiceDate) obj;
        return sequenceNumber == other.sequenceNumber;
    }

    /**
     * Adjust the supplied {@link Calendar} object such that the calendar will be
     * at "midnight" (12:00am) at the start of the day specified by the current
     * calendar date and locale. Note that we take the GTFS convention of
     * calculating midnight by setting the target date to noon (12:00pm) for the
     * service date and timezone specified and then subtracting twelve hours.
     * Normally that would be equivalent to midnight, except on Daylight Saving
     * Time days, in which case it can be an hour ahead or behind. This behavior
     * ensures correct calculation of {@link org.opentripplanner.model.StopTime}
     * arrival and departure time when the second offset is added to the localized
     * service date.
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


    /* Private Methods */

    private static Calendar getCalendarForDate(Date date) {
        Calendar c = Calendar.getInstance();
        c.setTime(date);
        return c;
    }

    private static void verifyIsInRange(int v, int min, int max, String name) {
        if(v < min || v > max) {
            throw new IllegalArgumentException(
                    "The ServiceDate " + name + " is not valid. The value " + v
                            + " is not in range [" + min + ", " + max + "]."
            );
        }
    }
}
