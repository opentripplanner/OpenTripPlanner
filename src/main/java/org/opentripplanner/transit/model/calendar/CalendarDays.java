package org.opentripplanner.transit.model.calendar;

import static java.time.ZoneOffset.UTC;

import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Objects;

/**
 * The purpose of this class is to hold information about the transit calendar OPERATION DAYS.
 * The information is:
 *  - time zone used for the calendar, may differ from the agency timezone
 *  - start and end date for all transit data
 *  -
 *  -
 *  - The exact
 *
 *
 *
 *
 */
public class CalendarDays {

  private static final ZoneId ZONE_ID = UTC;
  /**
   * We set the default start time for each day to 04:00 in the given zone.
   */
  private static final Duration TIME_OFFSET = Duration.ofHours(4);
  private static final LocalDate DEFAULT_START = LocalDate.now(ZONE_ID).minusMonths(3);
  private static final LocalDate DEFAULT_END = DEFAULT_START.plusYears(1);

  private final ZoneId zoneId;
  private final LocalDate start;
  private final LocalDate end;
  private final OperatingDay[] operatingDays;

  CalendarDays(CalendarDaysBuilder builder) {
    this.zoneId = Objects.requireNonNull(builder.zoneId());
    this.start = Objects.requireNonNull(builder.periodStart());
    this.end = Objects.requireNonNull(builder.periodEnd());
    Duration timeOffset = Objects.requireNonNull(builder.timeOffset());
    this.operatingDays = Objects.requireNonNull(builder.operatingDays());
  }

  public static CalendarDaysBuilder of() {
    return new CalendarDaysBuilder(ZONE_ID, DEFAULT_START, DEFAULT_END, TIME_OFFSET);
  }

  public ZoneId zoneId() {
    return zoneId;
  }

  public LocalDate start() {
    return start;
  }

  public LocalDate end() {
    return end;
  }

  public ZonedDateTime time(int dayIndex, int timeSeconds) {
    return operatingDays[dayIndex].startTime().plusSeconds(timeSeconds);
  }

  public int numberOfDays() {
    return operatingDays.length;
  }

  public int dayLengthSeconds(int dayIndex) {
    return operatingDays[dayIndex].lengthSeconds();
  }

  public OperatingDay operatingDay(int day) {
    return operatingDays[day];
  }
}
