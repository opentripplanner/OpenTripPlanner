package org.opentripplanner.ext.empiricaldelay.internal;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import javax.annotation.Nullable;
import org.opentripplanner.ext.empiricaldelay.EmpiricalDelayRepository;
import org.opentripplanner.ext.empiricaldelay.internal.model.EmpiricalDelaySummary;
import org.opentripplanner.ext.empiricaldelay.model.EmpiricalDelay;
import org.opentripplanner.ext.empiricaldelay.model.TripDelays;
import org.opentripplanner.ext.empiricaldelay.model.calendar.EmpiricalDelayCalendar;
import org.opentripplanner.transit.model.framework.FeedScopedId;

public class DefaultEmpiricalDelayRepository implements EmpiricalDelayRepository {

  /** feedId -> calendar */
  private final Map<String, EmpiricalDelayCalendar> serviceCalendars = new HashMap<>();
  /** tripId -> service id -> delays */
  private final Map<FeedScopedId, TripDelays> empiricalDelays = new HashMap<>();

  @Override
  public Optional<EmpiricalDelay> findEmpiricalDelay(
    FeedScopedId tripId,
    LocalDate serviceDate,
    int stopPosInPattern
  ) {
    var serviceId = findServiceId(tripId.getFeedId(), serviceDate);
    if (serviceId.isEmpty()) {
      return Optional.empty();
    }

    var delays = empiricalDelays.get(tripId);
    if (delays == null) {
      return Optional.empty();
    }
    return delays.get(serviceId.get(), stopPosInPattern);
  }

  @Override
  public void addEmpiricalDelayServiceCalendar(String feedId, EmpiricalDelayCalendar calendar) {
    if (serviceCalendars.containsKey(feedId)) {
      throw new IllegalStateException(
        "Only one EmpiricalDelayServiceCalendar is supported per feed."
      );
    }
    serviceCalendars.put(feedId, calendar);
  }

  @Override
  public void addTripDelays(TripDelays tripDelays) {
    empiricalDelays.put(tripDelays.tripId(), tripDelays);
  }

  @Override
  public String summary() {
    var summary = new EmpiricalDelaySummary();
    serviceCalendars.keySet().stream().forEach(summary::incServiceCalendars);
    empiricalDelays.keySet().stream().forEach(summary::incTrips);
    return summary.summary();
  }

  @Nullable
  private Optional<String> findServiceId(String feedId, LocalDate serviceDate) {
    var calendar = serviceCalendars.get(feedId);
    if (calendar == null) {
      return Optional.empty();
    }
    return calendar.findServiceId(serviceDate);
  }
}
