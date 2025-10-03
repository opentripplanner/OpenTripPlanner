package org.opentripplanner.ext.empiricaldelay.model.calendar;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class EmpiricalDelayCalendarBuilder {

  private final Map<DayOfWeek, ServiceCalendarPeriod> calendarForDays = new HashMap<>();

  public EmpiricalDelayCalendar build() {
    return new EmpiricalDelayCalendar(calendarForDays);
  }

  public EmpiricalDelayCalendarBuilder with(
    String serviceId,
    Collection<DayOfWeek> days,
    LocalDate start,
    LocalDate end
  ) {
    var cal = new ServiceCalendarPeriod(serviceId, start, end);
    for (DayOfWeek day : days) {
      if (calendarForDays.containsKey(day)) {
        throw new IllegalStateException(
          "More than one serviceId is defined on " + day + ". This is not allowed."
        );
      }
      calendarForDays.put(day, cal);
    }
    return this;
  }
}
