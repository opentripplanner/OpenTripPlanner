/* This file is based on code copied from project OneBusAway, see the LICENSE file for further information. */
package org.opentripplanner.model.calendar.impl;

import org.opentripplanner.model.FeedScopedId;
import org.opentripplanner.model.calendar.CalendarService;
import org.opentripplanner.model.calendar.CalendarServiceData;
import org.opentripplanner.model.calendar.ServiceDate;

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
    public Set<FeedScopedId> getServiceIds() {
        return data.getServiceIds();
    }

    @Override
    public Set<ServiceDate> getServiceDatesForServiceId(FeedScopedId serviceId) {
        Set<ServiceDate> dates = new HashSet<>();
        List<ServiceDate> serviceDates = data.getServiceDatesForServiceId(serviceId);
        if (serviceDates != null) {
            dates.addAll(serviceDates);
        }
        return dates;
    }

    @Override
    public Set<FeedScopedId> getServiceIdsOnDate(ServiceDate date) {
        return data.getServiceIdsForDate(date);
    }

    @Override
    public TimeZone getTimeZoneForAgencyId(FeedScopedId agencyId) {
        return data.getTimeZoneForAgencyId(agencyId);
    }

    /**
     * Get or create a serviceId for a given date. This method is used when a new trip is
     * added from during realtime data updates.
     *
     * TODO OTP2 - This is NOT THREAD-SAFE and is used in the real-time updaters, we need to fix
     *           - this when doing the issue #3030.
     *
     * @param serviceDate service date for the added service id
     */
    public FeedScopedId getOrCreateServiceIdForDate(ServiceDate serviceDate) {
        return data.getOrCreateServiceIdForDate(serviceDate);
    }
}
