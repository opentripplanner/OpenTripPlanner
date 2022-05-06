package org.opentripplanner.transit.model.calendar;

import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;

/**
 * This calendar contains timetables for each day and pattern. The switch from one day to another
 * is an arbitrary time during the day. Note that the service day length might differ from the
 * normal 24h. It can be 23h or 25h due to adjusting the time for daylight savings or 23:59:59 or
 * 24:00:01 when adjusting for leap seconds.
 */
public class ServiceCalendar {

  private final ZonedDateTime startTime;
  private final ServiceDay[] serviceDays;
  private int[] timeOffsets;

  public ServiceCalendar(ZonedDateTime startTime, ServiceDay[] serviceDays, int[] timeOffsets) {
    this.startTime = startTime;
    this.serviceDays = serviceDays;
    this.timeOffsets = timeOffsets;
  }

  /**
   * The time witch the fist service day start, for example 2022-01-31T04:00:00+01:00 Europe/Paris.
   * It is encouraged to use a time early in the morning where the number of running trips and
   * travelers are at a minimum. For example 04:00 in the morning is normally a good time to divide
   * up the timetables.
   */
  public ZonedDateTime getStartTime() {
    return startTime;
  }

  public int timeOffsets(int serviceDay) {
    return timeOffsets[serviceDay];
  }

  public int currentDate(Instant time) {
    return (int) ChronoUnit.DAYS.between(startTime, time);
  }

  TimetableCalendar timetables(int patternIndex, int currentDay) {
    return new TimetableCalendar(this, patternIndex, currentDay);
  }

  int numberOfDays() {
    return serviceDays.length;
  }

  ServiceDay serviceDay(int day) {
    return serviceDays[day];
  }
}
