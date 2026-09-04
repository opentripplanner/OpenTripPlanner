package org.opentripplanner.gtfs.mapping;

import java.time.DayOfWeek;
import java.util.Collection;
import java.util.EnumSet;
import java.util.Set;
import org.onebusaway.gtfs.model.ServiceCalendar;
import org.opentripplanner.transit.model.calendar.TripCalendarsBuilder;

/** Responsible for mapping GTFS ServiceCalendar rows into the OTP model. */
class ServiceCalendarMapper {

  private final IdFactory idFactory;

  ServiceCalendarMapper(IdFactory idFactory) {
    this.idFactory = idFactory;
  }

  void map(Collection<ServiceCalendar> allServiceCalendars, TripCalendarsBuilder calendars) {
    if (allServiceCalendars == null) {
      return;
    }
    allServiceCalendars.forEach(c -> map(c, calendars));
  }

  /** Map from GTFS to OTP model, {@code null} safe. */
  void map(ServiceCalendar rhs, TripCalendarsBuilder calendars) {
    if (rhs == null) {
      return;
    }
    calendars.addWeeklyCalendar(
      idFactory.createId(rhs.getServiceId(), "service calendar"),
      daysOfWeek(rhs),
      ServiceDateMapper.mapServiceDateInterval(rhs.getStartDate(), rhs.getEndDate())
    );
  }

  private static Set<DayOfWeek> daysOfWeek(ServiceCalendar c) {
    Set<DayOfWeek> days = EnumSet.noneOf(DayOfWeek.class);
    if (c.getMonday() == 1) {
      days.add(DayOfWeek.MONDAY);
    }
    if (c.getTuesday() == 1) {
      days.add(DayOfWeek.TUESDAY);
    }
    if (c.getWednesday() == 1) {
      days.add(DayOfWeek.WEDNESDAY);
    }
    if (c.getThursday() == 1) {
      days.add(DayOfWeek.THURSDAY);
    }
    if (c.getFriday() == 1) {
      days.add(DayOfWeek.FRIDAY);
    }
    if (c.getSaturday() == 1) {
      days.add(DayOfWeek.SATURDAY);
    }
    if (c.getSunday() == 1) {
      days.add(DayOfWeek.SUNDAY);
    }
    return days;
  }
}
