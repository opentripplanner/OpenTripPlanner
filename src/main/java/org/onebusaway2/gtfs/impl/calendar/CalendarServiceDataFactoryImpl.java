/**
 * Copyright (C) 2011 Brian Ferris <bdferris@onebusaway.org>
 * Copyright (C) 2012 Google, Inc.
 * Copyright (C) 2013 Codemass, Inc. <aaron@codemass.com>
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
package org.onebusaway.gtfs.impl.calendar;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TimeZone;

import org.onebusaway.gtfs.model.Agency;
import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.gtfs.model.ServiceCalendar;
import org.onebusaway.gtfs.model.ServiceCalendarDate;
import org.onebusaway.gtfs.model.calendar.CalendarServiceData;
import org.onebusaway.gtfs.model.calendar.LocalizedServiceId;
import org.onebusaway.gtfs.model.calendar.ServiceDate;
import org.onebusaway.gtfs.services.GtfsRelationalDao;
import org.onebusaway.gtfs.services.calendar.CalendarService;
import org.onebusaway.gtfs.services.calendar.CalendarServiceDataFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * We perform initial date calculations in the timezone of the host jvm, which
 * may be different than the timezone of an agency with the specified service
 * id. To my knowledge, the calculation should work the same, which is to say I
 * can't immediately think of any cases where the service dates would be
 * computed incorrectly.
 * 
 * @author bdferris
 */
public class CalendarServiceDataFactoryImpl implements
    CalendarServiceDataFactory {

  private final Logger _log = LoggerFactory.getLogger(CalendarServiceDataFactoryImpl.class);

  private GtfsRelationalDao _dao;

  private int _excludeFutureServiceDatesInDays;

  public static CalendarService createService(GtfsRelationalDao dao) {
    CalendarServiceDataFactoryImpl factory = new CalendarServiceDataFactoryImpl(
        dao);
    return new CalendarServiceImpl(factory.createData());
  }

  public CalendarServiceDataFactoryImpl() {

  }

  public CalendarServiceDataFactoryImpl(GtfsRelationalDao dao) {
    _dao = dao;
  }

  public void setGtfsDao(GtfsRelationalDao dao) {
    _dao = dao;
  }

  public void setExcludeFutureServiceDatesInDays(
      int excludeFutureServiceDatesInDays) {
    _excludeFutureServiceDatesInDays = excludeFutureServiceDatesInDays;
  }

  @Override
  public CalendarServiceData createData() {

    CalendarServiceData data = new CalendarServiceData();

    setTimeZonesForAgencies(data);

    List<AgencyAndId> serviceIds = _dao.getAllServiceIds();

    int index = 0;

    for (AgencyAndId serviceId : serviceIds) {

      index++;

      _log.info("serviceId=" + serviceId + " (" + index + "/"
          + serviceIds.size() + ")");

      TimeZone serviceIdTimeZone = data.getTimeZoneForAgencyId(serviceId.getAgencyId());
      if (serviceIdTimeZone == null) {
        serviceIdTimeZone = TimeZone.getDefault();
      }

      Set<ServiceDate> activeDates = getServiceDatesForServiceId(serviceId,
          serviceIdTimeZone);

      List<ServiceDate> serviceDates = new ArrayList<ServiceDate>(activeDates);
      Collections.sort(serviceDates);

      data.putServiceDatesForServiceId(serviceId, serviceDates);

      List<String> tripAgencyIds = _dao.getTripAgencyIdsReferencingServiceId(serviceId);

      Set<TimeZone> timeZones = new HashSet<TimeZone>();
      for (String tripAgencyId : tripAgencyIds) {
        TimeZone timeZone = data.getTimeZoneForAgencyId(tripAgencyId);
        timeZones.add(timeZone);
      }

      for (TimeZone timeZone : timeZones) {

        List<Date> dates = new ArrayList<Date>(serviceDates.size());
        for (ServiceDate serviceDate : serviceDates)
          dates.add(serviceDate.getAsDate(timeZone));

        LocalizedServiceId id = new LocalizedServiceId(serviceId, timeZone);
        data.putDatesForLocalizedServiceId(id, dates);
      }
    }

    return data;
  }

  public Set<ServiceDate> getServiceDatesForServiceId(AgencyAndId serviceId,
      TimeZone serviceIdTimeZone) {
    Set<ServiceDate> activeDates = new HashSet<ServiceDate>();
    ServiceCalendar c = _dao.getCalendarForServiceId(serviceId);

    if (c != null) {
      addDatesFromCalendar(c, serviceIdTimeZone, activeDates);
    }
    for (ServiceCalendarDate cd : _dao.getCalendarDatesForServiceId(serviceId)) {
      addAndRemoveDatesFromCalendarDate(cd, serviceIdTimeZone, activeDates);
    }
    return activeDates;
  }

  private void setTimeZonesForAgencies(CalendarServiceData data) {
    for (Agency agency : _dao.getAllAgencies()) {
      TimeZone timeZone = TimeZone.getTimeZone(agency.getTimezone());
      if (timeZone.getID().equals("GMT")
          && !agency.getTimezone().toUpperCase().equals("GMT")) {
        throw new UnknownAgencyTimezoneException(agency.getName(),
            agency.getTimezone());
      }
      data.putTimeZoneForAgencyId(agency.getId(), timeZone);
    }
  }

  private void addDatesFromCalendar(ServiceCalendar calendar,
      TimeZone timeZone, Set<ServiceDate> activeDates) {

    /**
     * We calculate service dates relative to noon so as to avoid any weirdness
     * relative to DST.
     */
    Date startDate = getServiceDateAsNoon(calendar.getStartDate(), timeZone);
    Date endDate = getServiceDateAsNoon(calendar.getEndDate(), timeZone);

    java.util.Calendar c = java.util.Calendar.getInstance(timeZone);
    c.setTime(startDate);

    while (true) {
      Date date = c.getTime();
      if (date.after(endDate))
        break;

      int day = c.get(java.util.Calendar.DAY_OF_WEEK);
      boolean active = false;

      switch (day) {
        case java.util.Calendar.MONDAY:
          active = calendar.getMonday() == 1;
          break;
        case java.util.Calendar.TUESDAY:
          active = calendar.getTuesday() == 1;
          break;
        case java.util.Calendar.WEDNESDAY:
          active = calendar.getWednesday() == 1;
          break;
        case java.util.Calendar.THURSDAY:
          active = calendar.getThursday() == 1;
          break;
        case java.util.Calendar.FRIDAY:
          active = calendar.getFriday() == 1;
          break;
        case java.util.Calendar.SATURDAY:
          active = calendar.getSaturday() == 1;
          break;
        case java.util.Calendar.SUNDAY:
          active = calendar.getSunday() == 1;
          break;
      }

      if (active) {
        addServiceDate(activeDates, new ServiceDate(c), timeZone);
      }

      c.add(java.util.Calendar.DAY_OF_YEAR, 1);
    }
  }

  private void addAndRemoveDatesFromCalendarDate(
      ServiceCalendarDate calendarDate, TimeZone serviceIdTimeZone,
      Set<ServiceDate> activeDates) {

    ServiceDate serviceDate = calendarDate.getDate();
    Date targetDate = calendarDate.getDate().getAsDate();
    Calendar c = Calendar.getInstance();
    c.setTime(targetDate);

    switch (calendarDate.getExceptionType()) {
      case ServiceCalendarDate.EXCEPTION_TYPE_ADD:
        addServiceDate(activeDates, serviceDate, serviceIdTimeZone);
        break;
      case ServiceCalendarDate.EXCEPTION_TYPE_REMOVE:
        activeDates.remove(serviceDate);
        break;
      default:
        _log.warn("unknown CalendarDate exception type: "
            + calendarDate.getExceptionType());
        break;
    }
  }

  private void addServiceDate(Set<ServiceDate> activeDates,
      ServiceDate serviceDate, TimeZone timeZone) {
    if (_excludeFutureServiceDatesInDays > 0) {
      int days = (int) ((serviceDate.getAsDate().getTime() - System.currentTimeMillis()) / (24 * 60 * 60 * 1000));
      if (days > _excludeFutureServiceDatesInDays)
        return;
    }

    activeDates.add(new ServiceDate(serviceDate));
  }

  private static Date getServiceDateAsNoon(ServiceDate serviceDate,
      TimeZone timeZone) {
    Calendar c = serviceDate.getAsCalendar(timeZone);
    c.add(Calendar.HOUR_OF_DAY, 12);
    return c.getTime();
  }
}
