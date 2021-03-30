/* This file is based on code copied from project OneBusAway, see the LICENSE file for further information. */
package org.opentripplanner.model.calendar;

import org.opentripplanner.model.FeedScopedId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import java.util.stream.Collectors;

public class CalendarServiceData implements Serializable {

    private static final long serialVersionUID = 1L;
    private static final String CAL_SERVICE_FEED_ID = "CSID";
    private static final Logger LOG = LoggerFactory.getLogger(CalendarServiceData.class);

    private final Map<FeedScopedId, TimeZone> timeZonesByAgencyId = new HashMap<>();

    private final Map<FeedScopedId, List<ServiceDate>> serviceDatesByServiceId = new HashMap<>();

    private final Map<ServiceDate, Set<FeedScopedId>> serviceIdsByDate = new HashMap<>();

    /**
     * @return the time zone for the specified agencyId, or null if the agency was
     *         not found
     */
    public TimeZone getTimeZoneForAgencyId(FeedScopedId agencyId) {
        return timeZonesByAgencyId.get(agencyId);
    }

    public void putTimeZoneForAgencyId(FeedScopedId agencyId, TimeZone timeZone) {
        timeZonesByAgencyId.put(agencyId, timeZone);
    }

    public Set<FeedScopedId> getAgencyIds() {
        return Collections.unmodifiableSet(timeZonesByAgencyId.keySet());
    }

    public Set<FeedScopedId> getServiceIds() {
        return Collections.unmodifiableSet(serviceDatesByServiceId.keySet());
    }

    public List<ServiceDate> getServiceDatesForServiceId(FeedScopedId serviceId) {
        return serviceDatesByServiceId.get(serviceId);
    }

    public Set<FeedScopedId> getServiceIdsForDate(ServiceDate date) {
        Set<FeedScopedId> serviceIds = serviceIdsByDate.get(date);
        if (serviceIds == null) {
            serviceIds = new HashSet<>();
        }
        return serviceIds;
    }

    public void putServiceDatesForServiceId(FeedScopedId serviceId, List<ServiceDate> dates) {
        List<ServiceDate> serviceDates = sortedImmutableList(dates);
        serviceDatesByServiceId.put(serviceId, serviceDates);
        addDatesToServiceIdsByDate(serviceId, serviceDates);
    }

    /**
     * TODO OTP2 - This is NOT THREAD-SAFE and is used in the real-time updaters, we need to fix
     *           - this when doing the issue #3030.
     */
    public FeedScopedId getOrCreateServiceIdForDate(ServiceDate serviceDate) {
        FeedScopedId serviceId = new FeedScopedId(CAL_SERVICE_FEED_ID, serviceDate.asCompactString());
        if(serviceDatesByServiceId.containsKey(serviceId)) {
            return serviceId;
        }
        serviceDatesByServiceId.put(serviceId, List.of(serviceDate));
        serviceIdsByDate.computeIfAbsent(serviceDate, d -> new HashSet<>()).add(serviceId);

        LOG.info("Adding serviceId {} to CalendarService", serviceId);

        return serviceId;
    }

    public void add(CalendarServiceData other) {
        for (FeedScopedId agencyId : other.getAgencyIds()) {
            putTimeZoneForAgencyId(agencyId, other.getTimeZoneForAgencyId(agencyId));
        }
        for (FeedScopedId serviceId : other.serviceDatesByServiceId.keySet()) {
            putServiceDatesForServiceId(serviceId, other.serviceDatesByServiceId.get(serviceId));
        }
    }


    /* private methods */

    private static <T> List<T> sortedImmutableList(Collection<T> c) {
        return Collections.unmodifiableList(c.stream().sorted().collect(Collectors.toList()));
    }

    private void addDatesToServiceIdsByDate(FeedScopedId serviceId, List<ServiceDate> serviceDates) {
        for (ServiceDate serviceDate : serviceDates) {
            Set<FeedScopedId> serviceIds = serviceIdsByDate.computeIfAbsent(
                    serviceDate,
                    k -> new HashSet<>()
            );
            serviceIds.add(serviceId);
        }
    }
}
