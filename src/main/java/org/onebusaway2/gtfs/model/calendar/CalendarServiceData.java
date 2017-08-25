/**
 * Copyright (C) 2011 Brian Ferris <bdferris@onebusaway.org>
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
package org.onebusaway.gtfs.model.calendar;

import java.io.Serializable;
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

public class CalendarServiceData implements Serializable {

  private static final long serialVersionUID = 1L;

  private Map<String, TimeZone> _timeZonesByAgencyId = new HashMap<String, TimeZone>();

  private Map<AgencyAndId, List<ServiceDate>> _serviceDatesByServiceId = new HashMap<AgencyAndId, List<ServiceDate>>();

  private Map<LocalizedServiceId, List<Date>> _datesByLocalizedServiceId = new HashMap<LocalizedServiceId, List<Date>>();

  private Map<ServiceDate, Set<AgencyAndId>> _serviceIdsByDate = new HashMap<ServiceDate, Set<AgencyAndId>>();

  /**
   * @param agencyId
   * @return the time zone for the specified agencyId, or null if the agency was
   *         not found
   */
  public TimeZone getTimeZoneForAgencyId(String agencyId) {
    return _timeZonesByAgencyId.get(agencyId);
  }

  public void putTimeZoneForAgencyId(String agencyId, TimeZone timeZone) {
    _timeZonesByAgencyId.put(agencyId, timeZone);
  }

  public Set<AgencyAndId> getServiceIds() {
    return Collections.unmodifiableSet(_serviceDatesByServiceId.keySet());
  }

  public Set<LocalizedServiceId> getLocalizedServiceIds() {
    return Collections.unmodifiableSet(_datesByLocalizedServiceId.keySet());
  }

  public List<ServiceDate> getServiceDatesForServiceId(AgencyAndId serviceId) {
    return _serviceDatesByServiceId.get(serviceId);
  }

  public Set<AgencyAndId> getServiceIdsForDate(ServiceDate date) {
    Set<AgencyAndId> serviceIds = _serviceIdsByDate.get(date);
    if (serviceIds == null)
      serviceIds = new HashSet<AgencyAndId>();
    return serviceIds;
  }

  public void putServiceDatesForServiceId(AgencyAndId serviceId,
      List<ServiceDate> serviceDates) {
    serviceDates = new ArrayList<ServiceDate>(serviceDates);
    Collections.sort(serviceDates);
    serviceDates = Collections.unmodifiableList(serviceDates);
    _serviceDatesByServiceId.put(serviceId, serviceDates);
    for (ServiceDate serviceDate : serviceDates) {
      Set<AgencyAndId> serviceIds = _serviceIdsByDate.get(serviceDate);
      if (serviceIds == null) {
        serviceIds = new HashSet<AgencyAndId>();
        _serviceIdsByDate.put(serviceDate, serviceIds);
      }
      serviceIds.add(serviceId);
    }
  }

  public List<Date> getDatesForLocalizedServiceId(LocalizedServiceId serviceId) {
    return _datesByLocalizedServiceId.get(serviceId);
  }

  public void putDatesForLocalizedServiceId(LocalizedServiceId serviceId,
      List<Date> dates) {
    dates = Collections.unmodifiableList(new ArrayList<Date>(dates));
    _datesByLocalizedServiceId.put(serviceId, dates);
  }

  public void makeReadOnly() {
    _timeZonesByAgencyId = Collections.unmodifiableMap(_timeZonesByAgencyId);
    _serviceDatesByServiceId = Collections.unmodifiableMap(_serviceDatesByServiceId);
    _datesByLocalizedServiceId = Collections.unmodifiableMap(_datesByLocalizedServiceId);
    _serviceIdsByDate = Collections.unmodifiableMap(_serviceIdsByDate);
  }
}
