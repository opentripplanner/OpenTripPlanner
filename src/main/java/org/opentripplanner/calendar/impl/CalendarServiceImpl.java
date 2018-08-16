/* This file is based on code copied from project OneBusAway, see the LICENSE file for further information. */
package org.opentripplanner.calendar.impl;

import org.opentripplanner.model.FeedId;
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
    public Set<FeedId> getServiceIds() {
        return data.getServiceIds();
    }

    @Override
    public Set<ServiceDate> getServiceDatesForServiceId(FeedId serviceId) {
        Set<ServiceDate> dates = new HashSet<>();
        CalendarServiceData allData = getData();
        List<ServiceDate> serviceDates = allData.getServiceDatesForServiceId(serviceId);
        if (serviceDates != null)
            dates.addAll(serviceDates);
        return dates;
    }

    @Override
    public Set<FeedId> getServiceIdsOnDate(ServiceDate date) {
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
