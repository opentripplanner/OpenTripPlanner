/* This file is based on code copied from project OneBusAway, see the LICENSE file for further information. */
package org.opentripplanner.model.calendar.impl;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.opentripplanner.model.calendar.CalendarService;
import org.opentripplanner.model.calendar.CalendarServiceData;
import org.opentripplanner.transit.model.framework.FeedScopedId;

/**
 * An implementation of {@link CalendarService}. Requires a pre-computed {@link CalendarServiceData}
 * bundle for efficient operation.
 *
 * @author bdferris
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
  public Set<LocalDate> getServiceDatesForServiceId(FeedScopedId serviceId) {
    Set<LocalDate> dates = new HashSet<>();
    List<LocalDate> serviceDates = data.getServiceDatesForServiceId(serviceId);
    if (serviceDates != null) {
      dates.addAll(serviceDates);
    }
    return dates;
  }

  @Override
  public Set<FeedScopedId> getServiceIdsOnDate(LocalDate date) {
    return data.getServiceIdsForDate(date);
  }

  /**
   * Get or create a serviceId for a given date. This method is used when a new trip is added from
   * during realtime data updates.
   * <p>
   * TODO OTP2 - This is NOT THREAD-SAFE and is used in the real-time updaters, we need to fix
   *           - this when doing the issue #3030.
   *
   * @param serviceDate service date for the added service id
   */
  public FeedScopedId getOrCreateServiceIdForDate(LocalDate serviceDate) {
    return data.getOrCreateServiceIdForDate(serviceDate);
  }
}
