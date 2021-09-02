package org.opentripplanner.api.resource;

import org.opentripplanner.model.FeedScopedId;
import org.opentripplanner.model.calendar.CalendarServiceData;
import org.opentripplanner.model.calendar.ServiceDate;

import java.util.Set;
import java.util.TimeZone;

/**
 * This class extends the {@link CalendarServiceData} class to allow for easier testing.
 * It includes methods to return both the set of service ids and the time zone used for testing.
 */
public class CalendarServiceDataStub extends CalendarServiceData {
    private static final long serialVersionUID = 1L;

    private final Set<FeedScopedId> serviceIds;
    private final TimeZone timeZone;

    public CalendarServiceDataStub(Set<FeedScopedId> serviceIds, TimeZone timeZone) {
        this.serviceIds = serviceIds;
        this.timeZone = timeZone;
    }

    @Override
    public Set<FeedScopedId> getServiceIds() {
        return serviceIds;
    }

    @Override
    public Set<FeedScopedId> getServiceIdsForDate(ServiceDate date) {
        return serviceIds;
    }

    @Override
    public TimeZone getTimeZoneForAgencyId(String agencyId) {
        return timeZone;
    }
}
