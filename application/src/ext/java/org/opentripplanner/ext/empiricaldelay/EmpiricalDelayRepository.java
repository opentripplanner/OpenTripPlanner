package org.opentripplanner.ext.empiricaldelay;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.Optional;
import org.opentripplanner.ext.empiricaldelay.model.EmpiricalDelay;
import org.opentripplanner.ext.empiricaldelay.model.TripDelays;
import org.opentripplanner.ext.empiricaldelay.model.calendar.EmpiricalDelayCalendar;
import org.opentripplanner.transit.model.framework.FeedScopedId;

/**
 * Repository for empirical delay data.
 */
public interface EmpiricalDelayRepository extends Serializable {
  /**
   * Return the empirical delay for the given serviceId and stop position.
   */
  Optional<EmpiricalDelay> findEmpiricalDelay(
    FeedScopedId tripId,
    LocalDate serviceDate,
    int stopPosInPattern
  );

  /**
   * Add a calendar for the given feed-id to the repository. The clendar MUST be added
   * BEFORE any trip-time-delays for the same feed is added.
   */
  void addEmpiricalDelayServiceCalendar(String feedId, EmpiricalDelayCalendar calendar);

  /** Add delays for a given trip to the repository. */
  void addTripDelays(TripDelays tripDelays);

  String summary();
}
