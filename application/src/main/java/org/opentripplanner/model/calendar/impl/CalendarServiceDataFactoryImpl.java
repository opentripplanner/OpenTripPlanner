/* This file is based on code copied from project OneBusAway, see the LICENSE file for further information. */
package org.opentripplanner.model.calendar.impl;

import static java.util.stream.Collectors.groupingBy;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.opentripplanner.model.calendar.CalendarServiceData;
import org.opentripplanner.model.calendar.ServiceCalendar;
import org.opentripplanner.model.calendar.ServiceCalendarDate;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * We perform initial date calculations in the timezone of the host jvm, which may be different than
 * the timezone of an agency with the specified service id. To my knowledge, the calculation should
 * work the same, which is to say I can't immediately think of any cases where the service dates
 * would be computed incorrectly.
 *
 * @author bdferris
 */
public class CalendarServiceDataFactoryImpl {

  private static final Logger LOG = LoggerFactory.getLogger(CalendarServiceDataFactoryImpl.class);

  private final Map<FeedScopedId, List<ServiceCalendarDate>> calendarDatesByServiceId;
  private final Map<FeedScopedId, List<ServiceCalendar>> calendarsByServiceId;
  private final Set<FeedScopedId> serviceIds;

  private CalendarServiceDataFactoryImpl(
    Collection<ServiceCalendarDate> calendarDates,
    Collection<ServiceCalendar> serviceCalendars
  ) {
    this.calendarDatesByServiceId = calendarDates
      .stream()
      .collect(groupingBy(ServiceCalendarDate::getServiceId));
    this.calendarsByServiceId = serviceCalendars
      .stream()
      .collect(groupingBy(ServiceCalendar::getServiceId));
    this.serviceIds = merge(calendarDatesByServiceId.keySet(), calendarsByServiceId.keySet());
  }

  public static CalendarServiceData createCalendarServiceData(
    Collection<ServiceCalendarDate> calendarDates,
    Collection<ServiceCalendar> serviceCalendars
  ) {
    return new CalendarServiceDataFactoryImpl(calendarDates, serviceCalendars).createData();
  }

  /** package local to be unit testable */
  static <T> Set<T> merge(Collection<T> set1, Collection<T> set2) {
    Set<T> newSet = new HashSet<>();
    newSet.addAll(set1);
    newSet.addAll(set2);
    return newSet;
  }

  private CalendarServiceData createData() {
    CalendarServiceData data = new CalendarServiceData();

    int index = 0;

    for (FeedScopedId serviceId : serviceIds) {
      index++;

      LOG.debug("serviceId={} ({}/{})", serviceId, index, serviceIds.size());

      Set<LocalDate> activeDates = getServiceDatesForServiceId(serviceId);
      List<LocalDate> serviceDates = new ArrayList<>(activeDates);
      Collections.sort(serviceDates);

      data.putServiceDatesForServiceId(serviceId, serviceDates);
    }

    return data;
  }

  private Set<LocalDate> getServiceDatesForServiceId(FeedScopedId serviceId) {
    Set<LocalDate> activeDates = new HashSet<>();
    ServiceCalendar c = findCalendarForServiceId(serviceId);

    if (c != null) {
      addDatesFromCalendar(c, activeDates);
    }
    List<ServiceCalendarDate> dates = calendarDatesByServiceId.get(serviceId);
    if (dates != null) {
      for (ServiceCalendarDate cd : dates) {
        addAndRemoveDatesFromCalendarDate(cd, activeDates);
      }
    }
    return activeDates;
  }

  private ServiceCalendar findCalendarForServiceId(FeedScopedId serviceId) {
    List<ServiceCalendar> calendars = calendarsByServiceId.get(serviceId);

    if (calendars == null || calendars.isEmpty()) {
      return null;
    }
    if (calendars.size() == 1) {
      return calendars.get(0);
    }
    throw new MultipleCalendarsForServiceIdException(serviceId);
  }

  private void addDatesFromCalendar(ServiceCalendar calendar, Set<LocalDate> activeDates) {
    LocalDate startDate = calendar.getPeriod().getStart();
    LocalDate endDate = calendar.getPeriod().getEnd();

    for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
      if (
        switch (date.getDayOfWeek()) {
          case MONDAY -> calendar.getMonday() == 1;
          case TUESDAY -> calendar.getTuesday() == 1;
          case WEDNESDAY -> calendar.getWednesday() == 1;
          case THURSDAY -> calendar.getThursday() == 1;
          case FRIDAY -> calendar.getFriday() == 1;
          case SATURDAY -> calendar.getSaturday() == 1;
          case SUNDAY -> calendar.getSunday() == 1;
        }
      ) {
        addServiceDate(activeDates, date);
      }
    }
  }

  private void addAndRemoveDatesFromCalendarDate(
    ServiceCalendarDate calendarDate,
    Set<LocalDate> activeDates
  ) {
    LocalDate serviceDate = calendarDate.getDate();

    switch (calendarDate.getExceptionType()) {
      case ServiceCalendarDate.EXCEPTION_TYPE_ADD -> addServiceDate(activeDates, serviceDate);
      case ServiceCalendarDate.EXCEPTION_TYPE_REMOVE -> activeDates.remove(serviceDate);
      default -> LOG.warn(
        "unknown CalendarDate exception type: {}",
        calendarDate.getExceptionType()
      );
    }
  }

  private void addServiceDate(Set<LocalDate> activeDates, LocalDate serviceDate) {
    activeDates.add(serviceDate);
  }
}
