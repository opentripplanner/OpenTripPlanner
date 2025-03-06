/* This file is based on code copied from project OneBusAway, see the LICENSE file for further information. */
package org.opentripplanner.model.calendar;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.utils.time.ServiceDateUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CalendarServiceData implements Serializable {

  private static final String CAL_SERVICE_FEED_ID = "CSID";
  private static final Logger LOG = LoggerFactory.getLogger(CalendarServiceData.class);

  private final Map<FeedScopedId, List<LocalDate>> serviceDatesByServiceId = new HashMap<>();

  private final Map<LocalDate, Set<FeedScopedId>> serviceIdsByDate = new HashMap<>();

  public Set<FeedScopedId> getServiceIds() {
    return Collections.unmodifiableSet(serviceDatesByServiceId.keySet());
  }

  public List<LocalDate> getServiceDatesForServiceId(FeedScopedId serviceId) {
    return serviceDatesByServiceId.get(serviceId);
  }

  public Set<FeedScopedId> getServiceIdsForDate(LocalDate date) {
    Set<FeedScopedId> serviceIds = serviceIdsByDate.get(date);
    if (serviceIds == null) {
      serviceIds = new HashSet<>();
    }
    return serviceIds;
  }

  public void putServiceDatesForServiceId(FeedScopedId serviceId, List<LocalDate> dates) {
    List<LocalDate> serviceDates = sortedImmutableList(dates);
    serviceDatesByServiceId.put(serviceId, serviceDates);
    addDatesToServiceIdsByDate(serviceId, serviceDates);
  }

  /**
   * TODO OTP2 - This is NOT THREAD-SAFE and is used in the real-time updaters, we need to fix
   *           - this when doing the issue #3030.
   */
  public FeedScopedId getOrCreateServiceIdForDate(LocalDate serviceDate) {
    FeedScopedId serviceId = new FeedScopedId(
      CAL_SERVICE_FEED_ID,
      ServiceDateUtils.asCompactString(serviceDate)
    );
    if (serviceDatesByServiceId.containsKey(serviceId)) {
      return serviceId;
    }
    serviceDatesByServiceId.put(serviceId, List.of(serviceDate));
    serviceIdsByDate.computeIfAbsent(serviceDate, d -> new HashSet<>()).add(serviceId);

    LOG.info("Adding serviceId {} to CalendarService", serviceId);

    return serviceId;
  }

  public void add(CalendarServiceData other) {
    for (FeedScopedId serviceId : other.serviceDatesByServiceId.keySet()) {
      putServiceDatesForServiceId(serviceId, other.serviceDatesByServiceId.get(serviceId));
    }
  }

  public Optional<LocalDate> getFirstDate() {
    return serviceIdsByDate.keySet().stream().min(LocalDate::compareTo);
  }

  public Optional<LocalDate> getLastDate() {
    return serviceIdsByDate.keySet().stream().max(LocalDate::compareTo);
  }

  /* private methods */

  private static <T> List<T> sortedImmutableList(Collection<T> c) {
    return c.stream().sorted().toList();
  }

  private void addDatesToServiceIdsByDate(FeedScopedId serviceId, List<LocalDate> serviceDates) {
    for (LocalDate serviceDate : serviceDates) {
      Set<FeedScopedId> serviceIds = serviceIdsByDate.computeIfAbsent(serviceDate, k ->
        new HashSet<>()
      );
      serviceIds.add(serviceId);
    }
  }
}
