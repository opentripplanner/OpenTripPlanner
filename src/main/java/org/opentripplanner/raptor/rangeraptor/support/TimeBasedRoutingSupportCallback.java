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

  /**
   * This method allow the strategy to replace the existing boarding (if it exists) with a better
   * option. It is left to the implementation to check that a boarding already exist.
   *
   * @param earliestBoardTime - the earliest possible time a boarding can take place
   * @param stopPos           - the pattern stop position
   * @param stopIndex         - the global stop index
   */
  default void boardSameTrip(int earliestBoardTime, int stopPos, int stopIndex) {
    // Do nothing. For standard and multi-criteria Raptor we do not need to do anything.
  }
}
