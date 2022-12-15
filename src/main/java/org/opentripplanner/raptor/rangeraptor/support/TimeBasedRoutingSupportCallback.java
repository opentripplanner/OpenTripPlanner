package org.opentripplanner.raptor.rangeraptor.support;

import org.opentripplanner.raptor.spi.RaptorTripSchedule;
import org.opentripplanner.raptor.spi.RaptorTripScheduleBoardOrAlightEvent;

public interface TimeBasedRoutingSupportCallback<T extends RaptorTripSchedule> {
  /**
   * Board the given trip(event) at the given stop index.
   *
   * @param earliestBoardTime used to calculate wait-time (if needed)
   */
  void board(
    final int stopIndex,
    final int earliestBoardTime,
    RaptorTripScheduleBoardOrAlightEvent<T> boarding
  );
}
