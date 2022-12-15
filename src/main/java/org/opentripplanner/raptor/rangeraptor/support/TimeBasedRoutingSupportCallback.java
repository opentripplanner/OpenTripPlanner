package org.opentripplanner.raptor.rangeraptor.support;

import org.opentripplanner.raptor.spi.RaptorTripSchedule;
import org.opentripplanner.raptor.spi.RaptorTripScheduleBoardOrAlightEvent;

public interface TimeBasedRoutingSupportCallback<T extends RaptorTripSchedule> {
  /**
   * Board the given trip(event) at the given stop index.
   */
  void board(final int stopIndex, RaptorTripScheduleBoardOrAlightEvent<T> boarding);
}
