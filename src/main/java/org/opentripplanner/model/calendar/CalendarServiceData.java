/* This file is based on code copied from project OneBusAway, see the LICENSE file for further information. */
package org.opentripplanner.model.calendar;

import org.opentripplanner.model.FeedScopedId;

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

public class CalendarServiceData implements Serializable {

    private static final long serialVersionUID = 1L;

    private Map<String, TimeZone> timeZonesByAgencyId = new HashMap<>();

    private Map<FeedScopedId, List<ServiceDate>> serviceDatesByServiceId = new HashMap<>();

    private Map<LocalizedServiceId, List<Date>> datesByLocalizedServiceId = new HashMap<>();

    private Map<ServiceDate, Set<FeedScopedId>> serviceIdsByDate = new HashMap<>();

    /**
     * @param agencyId
     * @return the time zone for the specified agencyId, or null if the agency was
     *         not found
     */
    public TimeZone getTimeZoneForAgencyId(String agencyId) {
        return timeZonesByAgencyId.get(agencyId);
    }

    public void putTimeZoneForAgencyId(String agencyId, TimeZone timeZone) {
        timeZonesByAgencyId.put(agencyId, timeZone);
    }

    public Set<FeedScopedId> getServiceIds() {
        return Collections.unmodifiableSet(serviceDatesByServiceId.keySet());
    }

    public Set<LocalizedServiceId> getLocalizedServiceIds() {
        return Collections.unmodifiableSet(datesByLocalizedServiceId.keySet());
    }

    public List<ServiceDate> getServiceDatesForServiceId(FeedScopedId serviceId) {
        return serviceDatesByServiceId.get(serviceId);
    }

    public Set<FeedScopedId> getServiceIdsForDate(ServiceDate date) {
        Set<FeedScopedId> serviceIds = serviceIdsByDate.get(date);
        if (serviceIds == null)
            serviceIds = new HashSet<>();
        return serviceIds;
    }

    public void putServiceDatesForServiceId(FeedScopedId serviceId, List<ServiceDate> serviceDates) {
        serviceDates = new ArrayList<>(serviceDates);
        Collections.sort(serviceDates);
        serviceDates = Collections.unmodifiableList(serviceDates);
        serviceDatesByServiceId.put(serviceId, serviceDates);
        for (ServiceDate serviceDate : serviceDates) {
            Set<FeedScopedId> serviceIds = serviceIdsByDate.get(serviceDate);
            if (serviceIds == null) {
                serviceIds = new HashSet<>();
                serviceIdsByDate.put(serviceDate, serviceIds);
            }
            serviceIds.add(serviceId);
        }
    }

    public List<Date> getDatesForLocalizedServiceId(LocalizedServiceId serviceId) {
        return datesByLocalizedServiceId.get(serviceId);
    }

    public void putDatesForLocalizedServiceId(LocalizedServiceId serviceId, List<Date> dates) {
        dates = Collections.unmodifiableList(new ArrayList<>(dates));
        datesByLocalizedServiceId.put(serviceId, dates);
    }
}
