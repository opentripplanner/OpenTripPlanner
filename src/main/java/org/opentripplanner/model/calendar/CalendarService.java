package org.opentripplanner.model.calendar;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Set;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.transit.model.organization.Agency;

public interface CalendarService {
  /**
   * @return the set of all service ids used in the data set
   */
  Set<FeedScopedId> getServiceIds();

  /**
   * @param serviceId the target service id
   * @return the set of all service dates for which the specified service id is active
   */
  Set<LocalDate> getServiceDatesForServiceId(FeedScopedId serviceId);

  /**
   * Determine the set of service ids that are active on the specified service date.
   *
   * @param date the target service date
   * @return the set of service ids that are active on the specified service date
   */
  Set<FeedScopedId> getServiceIdsOnDate(LocalDate date);

  /**
   * Returns the {@link ZoneId} for the specified agency id
   *
   * @param agencyId {@link Agency#getId()}
   * @return the time zone for the specified agency, or null if the agency was not found
   */
  ZoneId getTimeZoneForAgencyId(FeedScopedId agencyId);
}
