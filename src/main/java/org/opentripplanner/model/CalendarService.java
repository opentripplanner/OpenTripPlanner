package org.opentripplanner.model;

import org.opentripplanner.model.calendar.ServiceDate;

import java.util.Set;
import java.util.TimeZone;

public interface CalendarService {

    /**
     * @return the set of all service ids used in the data set
     */
    Set<FeedScopedId> getServiceIds();

    /**
     * @param serviceId the target service id
     * @return the set of all service dates for which the specified service id is
     *         active
     */
    Set<ServiceDate> getServiceDatesForServiceId(FeedScopedId serviceId);

    /**
     * Determine the set of service ids that are active on the specified service
     * date.
     *
     * @param date the target service date
     * @return the set of service ids that are active on the specified service
     * date
     */
    Set<FeedScopedId> getServiceIdsOnDate(ServiceDate date);

    /**
     * Returns the instantiated {@link TimeZone} for the specified agency id
     *
     * @param agencyId {@link Agency#getId()}
     * @return the time zone for the specified agency, or null if the agency was
     * not found
     */
    TimeZone getTimeZoneForAgencyId(String agencyId);
}
