/**
 * Copyright (C) 2011 Brian Ferris <bdferris@onebusaway.org>
 * Copyright (C) 2012 Google, Inc.
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
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;

import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.gtfs.model.calendar.CalendarServiceData;
import org.onebusaway.gtfs.model.calendar.LocalizedServiceId;
import org.onebusaway.gtfs.model.calendar.ServiceDate;
import org.onebusaway.gtfs.model.calendar.ServiceIdIntervals;
import org.onebusaway.gtfs.model.calendar.ServiceInterval;
import org.onebusaway.gtfs.services.calendar.CalendarService;
import org.onebusaway.gtfs.services.calendar.CalendarServiceDataFactory;

/**
 * An implementation of {@link CalendarService}. Requires a pre-computed
 * {@link CalendarServiceData} bundle for efficient operation.
 * 
 * @author bdferris
 * 
 */
public class CalendarServiceImpl implements CalendarService {

  private CalendarServiceDataFactory _factory;

  private volatile CalendarServiceData _data;

  public CalendarServiceImpl() {

  }

  public CalendarServiceImpl(CalendarServiceDataFactory factory) {
    _factory = factory;
  }

  public CalendarServiceImpl(CalendarServiceData data) {
    _data = data;
  }

  public void setDataFactory(CalendarServiceDataFactory factory) {
    _factory = factory;
  }

  public void setData(CalendarServiceData data) {
    _data = data;
  }

  /****
   * {@link CalendarService} Interface
   ****/

  @Override
  public Set<AgencyAndId> getServiceIds() {
    CalendarServiceData allData = getData();
    return allData.getServiceIds();
  }

  @Override
  public Set<ServiceDate> getServiceDatesForServiceId(AgencyAndId serviceId) {
    Set<ServiceDate> dates = new HashSet<ServiceDate>();
    CalendarServiceData allData = getData();
    List<ServiceDate> serviceDates = allData.getServiceDatesForServiceId(serviceId);
    if (serviceDates != null)
      dates.addAll(serviceDates);
    return dates;
  }

  @Override
  public Set<AgencyAndId> getServiceIdsOnDate(ServiceDate date) {
    CalendarServiceData allData = getData();
    return allData.getServiceIdsForDate(date);
  }

  @Override
  public TimeZone getTimeZoneForAgencyId(String agencyId) {
    CalendarServiceData data = getData();
    return data.getTimeZoneForAgencyId(agencyId);
  }

  @Override
  public LocalizedServiceId getLocalizedServiceIdForAgencyAndServiceId(
      String agencyId, AgencyAndId serviceId) {
    TimeZone timeZone = getTimeZoneForAgencyId(agencyId);
    if (timeZone == null)
      return null;
    return new LocalizedServiceId(serviceId, timeZone);
  }

  public List<Date> getDatesForLocalizedServiceId(
      LocalizedServiceId localizedServiceId) {
    CalendarServiceData data = getData();
    return list(data.getDatesForLocalizedServiceId(localizedServiceId));
  }

  @Override
  public boolean isLocalizedServiceIdActiveOnDate(
      LocalizedServiceId localizedServiceId, Date serviceDate) {

    // TODO : Make this more efficient?
    CalendarServiceData data = getData();
    List<Date> dates = data.getDatesForLocalizedServiceId(localizedServiceId);
    return Collections.binarySearch(dates, serviceDate) >= 0;
  }

  @Override
  public List<Date> getServiceDateArrivalsWithinRange(
      LocalizedServiceId serviceId, ServiceInterval interval, Date from, Date to) {
    return getServiceDates(getData(), serviceId, interval,
        ServiceIdOp.ARRIVAL_OP, to, from, false);
  }

  @Override
  public Map<LocalizedServiceId, List<Date>> getServiceDateArrivalsWithinRange(
      ServiceIdIntervals serviceIdIntervals, Date from, Date to) {
    return getServiceDates(serviceIdIntervals, ServiceIdOp.ARRIVAL_OP, to,
        from, false);
  }

  @Override
  public List<Date> getServiceDateDeparturesWithinRange(
      LocalizedServiceId serviceId, ServiceInterval interval, Date from, Date to) {
    return getServiceDates(getData(), serviceId, interval,
        ServiceIdOp.DEPARTURE_OP, from, to, false);
  }

  @Override
  public Map<LocalizedServiceId, List<Date>> getServiceDateDeparturesWithinRange(
      ServiceIdIntervals serviceIdIntervals, Date from, Date to) {
    return getServiceDates(serviceIdIntervals, ServiceIdOp.DEPARTURE_OP, from,
        to, false);
  }

  @Override
  public List<Date> getServiceDatesWithinRange(LocalizedServiceId serviceId,
      ServiceInterval interval, Date from, Date to) {
    return getServiceDates(getData(), serviceId, interval, ServiceIdOp.BOTH_OP,
        from, to, false);
  }

  @Override
  public Map<LocalizedServiceId, List<Date>> getServiceDatesWithinRange(
      ServiceIdIntervals serviceIdIntervals, Date from, Date to) {
    return getServiceDates(serviceIdIntervals, ServiceIdOp.BOTH_OP, from, to,
        false);
  }

  @Override
  public List<Date> getNextDepartureServiceDates(LocalizedServiceId serviceId,
      ServiceInterval interval, long targetTime) {
    Date target = new Date(targetTime);
    return getServiceDates(getData(), serviceId, interval,
        ServiceIdOp.DEPARTURE_OP, target, target, true);
  }

  @Override
  public Map<LocalizedServiceId, List<Date>> getNextDepartureServiceDates(
      ServiceIdIntervals serviceIdIntervals, long targetTime) {
    Date target = new Date(targetTime);
    return getServiceDates(serviceIdIntervals, ServiceIdOp.DEPARTURE_OP,
        target, target, true);
  }

  @Override
  public List<Date> getPreviousArrivalServiceDates(
      LocalizedServiceId serviceId, ServiceInterval interval, long targetTime) {
    Date target = new Date(targetTime);
    return getServiceDates(getData(), serviceId, interval,
        ServiceIdOp.ARRIVAL_OP, target, target, true);
  }

  @Override
  public Map<LocalizedServiceId, List<Date>> getPreviousArrivalServiceDates(
      ServiceIdIntervals serviceIdIntervals, long targetTime) {
    Date target = new Date(targetTime);
    return getServiceDates(serviceIdIntervals, ServiceIdOp.ARRIVAL_OP, target,
        target, true);
  }

  /****
   * Private Methods
   ****/

  protected CalendarServiceData getData() {
    if (_data == null) {
      synchronized (this) {
        if (_data == null) {
          _data = _factory.createData();
        }
      }
    }
    return _data;
  }

  private Map<LocalizedServiceId, List<Date>> getServiceDates(
      ServiceIdIntervals serviceIdIntervals, ServiceIdOp op, Date from,
      Date to, boolean includeNextDate) {

    CalendarServiceData allData = getData();

    Map<LocalizedServiceId, List<Date>> results = new HashMap<LocalizedServiceId, List<Date>>();

    for (Map.Entry<LocalizedServiceId, ServiceInterval> entry : serviceIdIntervals) {

      LocalizedServiceId serviceId = entry.getKey();
      ServiceInterval interval = entry.getValue();

      List<Date> serviceDates = getServiceDates(allData, serviceId, interval,
          op, from, to, includeNextDate);

      if (!serviceDates.isEmpty())
        results.put(serviceId, serviceDates);
    }

    return results;
  }

  private List<Date> getServiceDates(CalendarServiceData allData,
      LocalizedServiceId serviceId, ServiceInterval interval, ServiceIdOp op,
      Date from, Date to, boolean includeNextDateIfNeeded) {

    List<Date> serviceDates = allData.getDatesForLocalizedServiceId(serviceId);

    List<Date> resultsForServiceId = new ArrayList<Date>();
    Date nextDate = null;

    if (serviceDates == null)
      return resultsForServiceId;

    Date target = op.shiftTime(interval, from);
    int index = search(serviceDates, op, 0, serviceDates.size(), target);

    if (index == serviceDates.size())
      index--;

    while (0 <= index) {
      Date serviceDate = op.getServiceDate(serviceDates, index);
      int rc = op.compareInterval(interval, serviceDate, from, to);

      if (rc > 0) {
        nextDate = serviceDate;
      } else if (rc == 0) {
        resultsForServiceId.add(serviceDate);
      } else if (rc < 0) {
        break;
      }
      index--;
    }

    if (includeNextDateIfNeeded && resultsForServiceId.isEmpty()
        && nextDate != null)
      resultsForServiceId.add(nextDate);

    return resultsForServiceId;
  }

  private int search(List<Date> serviceDates, ServiceIdOp op, int indexFrom,
      int indexTo, Date key) {

    if (indexTo == indexFrom)
      return indexFrom;

    int index = (indexFrom + indexTo) / 2;

    Date serviceDate = op.getServiceDate(serviceDates, index);

    int rc = op.compare(key, serviceDate);

    if (rc == 0)
      return index;

    if (rc < 0)
      return search(serviceDates, op, indexFrom, index, key);
    else
      return search(serviceDates, op, index + 1, indexTo, key);
  }

  private static final <T> List<T> list(List<T> values) {
    if (values == null)
      return Collections.emptyList();
    return values;
  }
}
