package org.opentripplanner.ext.empiricaldelay.model.calendar;

import java.io.Serializable;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * This implementation of a Service Calendar is specific to the emperical dealy.
 * A feed can only have one {@link EmpiricalDelayCalendar} and the calendar can only
 * contain one service(id) for each day-of-week. Exceptions are not currently
 * supported,but it should be possible to add this with small changes to this class.
 * Am important constraint is that o secific date should map to only one service-id.
 * Remember the calendar is not ment to represent the trip calendar information, but
 * the empirical delay information for each day-og-week.
 */
public class EmpiricalDelayCalendar implements Serializable {

  private final EnumMap<DayOfWeek, ServiceCalendarPeriod> calendarForDays;

  EmpiricalDelayCalendar(Map<DayOfWeek, ServiceCalendarPeriod> calendarForDays) {
    this.calendarForDays = new EnumMap<DayOfWeek, ServiceCalendarPeriod>(calendarForDays);
  }

  public static EmpiricalDelayCalendarBuilder of() {
    return new EmpiricalDelayCalendarBuilder();
  }

  public Optional<String> findServiceId(LocalDate serviceDate) {
    var day = DayOfWeek.from(serviceDate);
    var cal = calendarForDays.get(day);
    return cal != null && cal.accept(serviceDate) ? Optional.of(cal.serviceId()) : Optional.empty();
  }

  /**
   * Return a set of all service ids in the calendar.
   */
  public List<String> listServiceIds() {
    return calendarForDays.values().stream().map(c -> c.serviceId()).distinct().toList();
  }
}
