package org.opentripplanner.transit.model.calendar;

import java.time.ZonedDateTime;
import java.util.Collection;
import java.util.Objects;

/**
 * This calendar contains timetables for each day and pattern. The switch from one day to another
 * is an arbitrary time during the day. Note that the service day length might differ from the
 * normal 24h. It can be 23h or 25h due to adjusting the time for daylight savings or 23:59:59 or
 * 24:00:01 when adjusting for leap seconds.
 */
public class TransitCalendar {

  /** Info about the calendar days */
  private final CalendarDays daysInfo;
  private final PatternsOnDays patternsOnDays;

  public TransitCalendar(CalendarDays daysInfo, PatternsOnDays patternsOnDays) {
    this.daysInfo = Objects.requireNonNull(daysInfo);
    this.patternsOnDays = Objects.requireNonNull(patternsOnDays);
  }

  /**
   * The time witch the fist service day start, for example 2022-01-31T04:00:00+01:00 Europe/Paris.
   * It is encouraged to use a time early in the morning where the number of running trips and
   * travelers are at a minimum. For example 04:00 in the morning is normally a good time to divide
   * up the timetables.
   */
  public ZonedDateTime getStartTime() {
    return daysInfo.time(0, 0);
  }

  public Collection<PatternOnDay> patternsOnDay(int day) {
    return patternsOnDays.patternsOnDay(day).patterns();
  }

  TripScheduleSearchOnDays timetables(int patternIndex, int currentDay) {
    // TODO RTM
    return null;
  }

  int numberOfDays() {
    return daysInfo.numberOfDays();
  }
}
