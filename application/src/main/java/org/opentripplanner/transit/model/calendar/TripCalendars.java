package org.opentripplanner.transit.model.calendar;

import java.time.LocalDate;
import java.util.Set;
import org.opentripplanner.core.model.id.FeedScopedId;

/**
 *
 *
 * TODO - Convert return types to List not Set.
 */
public interface TripCalendars {
  /**
   * @return all service ids used in the data set. The list
   */
  Set<FeedScopedId> listServiceIds();

  /**
   * @param serviceId the target service id
   * @return the set of all service dates for which the specified service id is active
   */
  Set<LocalDate> listServiceDates(FeedScopedId serviceId);

  /**
   * Determine the set of service ids that are active on the specified service date.
   *
   * @param serviceDate the target service date
   * @return the set of service ids that are active on the specified service date
   */
  Set<FeedScopedId> listServiceIdsOnServiceDate(LocalDate serviceDate);

  /**
   * Return the integer service code assigned to the given service id, or {@code null} if the
   * service id is not registered.
   * <p>
   * Service codes are small integers (0, 1, 2, …) allocated during graph build to enable
   * compact BitSet-based lookups in the Raptor routing engine instead of object comparisons.
   * The relationship with service id is 1-to-1.
   *
   * @return the integer code, or {@code null} if not found
   */
  Integer getServiceCode(FeedScopedId serviceId);
}
