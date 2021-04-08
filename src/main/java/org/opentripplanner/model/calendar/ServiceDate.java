/* This file is based on code copied from project OneBusAway, see the LICENSE file for further information. */
package org.opentripplanner.model.calendar;

import java.io.Serializable;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.ParseException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
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

    private static final String MAX_TEXT = "MAX";
    private static final String MIN_TEXT = "MIN";

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

    /**
     * @deprecated Convert to {@link java.time.ZonedDateTime} instead of old Calendar.
     */
    @Deprecated
    public ServiceDate(Calendar calendar) {
        this(
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH) + 1,
                calendar.get(Calendar.DAY_OF_MONTH)
        );
    }

    /**
     * Construct a ServiceDate from the specified {@link Date} object, using the
     * default {@link TimeZone} object for the current VM to localize the date.
     *
     * @deprecated This is potentially dangerous to use. The TimeZone on the graph
     *             can be different from the VM/server default.
     */
    @Deprecated
    public ServiceDate(Date date) {
        this(LocalDate.ofInstant(date.toInstant(), ZoneId.systemDefault()));
    }

    /**
     * @deprecated This is potentially dangerous to use. The TimeZone on the graph
     *             can be different from the server default.
     */
    @Deprecated
    public ServiceDate() {
        this(new Date());
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

        Matcher matcher = PATTERN.matcher(value);

        if (!matcher.matches()) {
            throw new ParseException("error parsing date: " + value, 0);
        }

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
     * Create a ZonedDateTime based on the current service date, time zone and seconds-offset.
     * This method add the offset seconds to the service date start time, witch is defined
     * to be NOON - 12 hours. This is midnight for most days, except days where the time is
     * adjusted for daylight saving time.
     */
    public ZonedDateTime toZonedDateTime(ZoneId zoneId, int secondsOffset) {
        var d = ZonedDateTime.of(year, month, day, 12, 0, 0, 0, zoneId);
        return d.minusHours(12).plusSeconds(secondsOffset);
    }


    /**
     * Add a given number of seconds to the service date and convert it to a new service date if it
     * the new time is on another date. The given time-zone is used to account for days witch
     * do not have 24 hours (switching between summer and winter time).
     */
    public ServiceDate plusSeconds(ZoneId zoneId, int seconds) {
        return new ServiceDate(toZonedDateTime(zoneId, seconds).toLocalDate());
    }

    /**
     * @return calls {@link #getAsDate(TimeZone)} with the default timezone for
     *         this VM
     * @deprecated This is potentially dangerous to use. The TimeZone on the graph
     *             can be diffrent from the server default.
     */
    @Deprecated
    public Date getAsDate() {
        return getAsDate(TimeZone.getDefault());
    }

    private LocalDate toLocalDate() {
        return LocalDate.of(year, month, day);
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
     * @deprecated Replace this method with a method that uses the new {@link java.time}
     *             library instead of the old {@link Calendar}.
     */
    @Deprecated
    public Calendar getAsCalendar(TimeZone timeZone) {
        Calendar c = Calendar.getInstance();
        c.setTimeZone(timeZone);
        c.set(Calendar.YEAR, year);
        c.set(Calendar.MONTH, month - 1);
        c.set(Calendar.DAY_OF_MONTH, day);

        // Initial set time to noon
        c.set(Calendar.HOUR_OF_DAY, 12);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);

        // Subtract 12 hours. Usually takes you to midnight, except on DST days
        c.add(Calendar.HOUR_OF_DAY, -12);

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
     * @param numberOfDays number of days to shift current value, negative values are accepted.
     *
     * @return the service date following the current service date by the
     *         specified number of days, or preceding if a negative number of days
     *         is specified
     */
    public ServiceDate shift(int numberOfDays) {
        if(numberOfDays == 0) { return this; }
        return new ServiceDate(toLocalDate().plusDays(numberOfDays));
    }

    /**
     * @return the number of days between this service date and the specified
     *         argument service date
     * @deprecated This method uses UTC TimeZone, should be replaced with a method that uses the
     *             graph TimeZone.
     */
    @Deprecated
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
        if(MAX_DATE.equals(this)) { return MAX_TEXT; }
        if(MIN_DATE.equals(this)) { return MIN_TEXT; }
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

    /* Private Methods */

    /** {@code min} and {@code max} are exclusive. */
    private static void verifyIsInRange(int v, int min, int max, String name) {
        if(v < min || v > max) {
            throw new IllegalArgumentException(
                    "The ServiceDate " + name + " is not valid. The value " + v
                            + " is not in range [" + min + ", " + max + "]."
            );
        }
    }
}
