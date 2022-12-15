package org.opentripplanner.raptor.rangeraptor.support;

import java.util.function.IntConsumer;
import org.opentripplanner.raptor.spi.RaptorTripSchedule;
import org.opentripplanner.raptor.spi.RaptorTripScheduleBoardOrAlightEvent;
import org.opentripplanner.raptor.spi.RaptorTripScheduleSearch;
import org.opentripplanner.raptor.spi.TransitArrival;

public interface TimeBasedRoutingSupportCallback<T extends RaptorTripSchedule> {
  /**
   * Get the current boarding previous transit arrival. This is used to look up any guaranteed
   * transfers.
   */
  TransitArrival<T> previousTransit(int boardStopIndex);

  /**
   * Board trip for each stopArrival (Std have only one "best" arrival, while Mc may have many).
   */
  void forEachBoarding(int stopIndex, IntConsumer prevStopArrivalTimeConsumer);

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
   * The trip search will use this index to search relative to an existing boarding. This make a
   * subsequent search faster since it must board an earlier trip, and the trip search can start at
   * the given onTripIndex. if not the current trip is used.
   * <p>
   * Return {@link RaptorTripScheduleSearch#UNBOUNDED_TRIP_INDEX} to if the tripIndex is unknown.
   */
  default int onTripIndex() {
    return RaptorTripScheduleSearch.UNBOUNDED_TRIP_INDEX;
  }

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
