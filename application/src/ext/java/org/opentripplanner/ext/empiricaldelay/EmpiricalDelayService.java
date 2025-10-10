package org.opentripplanner.ext.empiricaldelay;

import java.time.LocalDate;
import java.util.Optional;
import org.opentripplanner.ext.empiricaldelay.model.EmpiricalDelay;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.utils.lang.Sandbox;

/**
 * A service for getting empirical delays.
 */
@Sandbox
public interface EmpiricalDelayService {
  /**
   * Return the empirical delay for the given service date and stop position, if it exist.
   */
  Optional<EmpiricalDelay> findEmpiricalDelay(
    FeedScopedId tripId,
    LocalDate serviceDate,
    int stopPosInPattern
  );
}
