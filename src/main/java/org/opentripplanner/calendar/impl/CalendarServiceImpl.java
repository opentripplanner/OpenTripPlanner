/*
 * Copyright (C) 2011 Brian Ferris <bdferris@onebusaway.org>
 * Copyright (C) 2012 Google, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.opentripplanner.calendar.impl;

import org.opentripplanner.model.AgencyAndId;
import org.opentripplanner.model.calendar.CalendarServiceData;
import org.opentripplanner.model.calendar.ServiceDate;
import org.opentripplanner.model.CalendarService;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TimeZone;

/**
 * An implementation of {@link CalendarService}. Requires a pre-computed
 * {@link CalendarServiceData} bundle for efficient operation.
 *
 * @author bdferris
 *
 */
public class CalendarServiceImpl implements CalendarService {

    private final CalendarServiceData data;

    public CalendarServiceImpl(CalendarServiceData data) {
        this.data = data;
    }

    @Override
    public Set<AgencyAndId> getServiceIds() {
        return data.getServiceIds();
    }

    @Override
    public Set<ServiceDate> getServiceDatesForServiceId(AgencyAndId serviceId) {
        Set<ServiceDate> dates = new HashSet<>();
        CalendarServiceData allData = getData();
        List<ServiceDate> serviceDates = allData.getServiceDatesForServiceId(serviceId);
        if (serviceDates != null)
            dates.addAll(serviceDates);
        return dates;
    }

    @Override
    public Set<AgencyAndId> getServiceIdsOnDate(ServiceDate date) {
        return data.getServiceIdsForDate(date);
    }

    @Override
    public TimeZone getTimeZoneForAgencyId(String agencyId) {
        return data.getTimeZoneForAgencyId(agencyId);
    }


  /* Private Methods */

    protected CalendarServiceData getData() {
        return data;
    }
}
