package org.opentripplanner.raptor.rangeraptor.internalapi;

import org.opentripplanner.raptor.api.model.RaptorAccessEgress;
import org.opentripplanner.raptor.api.model.RaptorTripSchedule;
import org.opentripplanner.raptor.rangeraptor.RangeRaptorWorker;
import org.opentripplanner.raptor.spi.RaptorConstrainedBoardingSearch;
import org.opentripplanner.raptor.spi.RaptorTimeTable;

/**
 * Provides alternative implementations of some logic within the {@link RangeRaptorWorker}.
 *
 * @param <T> The TripSchedule type defined by the user of the raptor API.
 */
public interface RoutingStrategy<T extends RaptorTripSchedule> {
  /**
   * Sets the access time for the departure stop. This method is called for each access path in
   * every Raptor iteration. The access path can have more than one "leg"; hence the implementation
   * need to be aware of the round (Walk access in round 0, Flex with one leg in round 1, ...).
   *
   * @param iterationDepartureTime The current iteration departure time.
   */
  void setAccessToStop(RaptorAccessEgress accessPath, int iterationDepartureTime);

  /**
   * Prepare the {@link RoutingStrategy} to route using the {@link RaptorTimeTable}.
   */
  void prepareForTransitWith(RaptorTimeTable<T> timeTable);

  /**
   * Alight the current trip at the given stop with the arrival times.
   */
  void alight(final int stopIndex, final int stopPos, final int alightSlack);

  /**
   * Board the given trip(event) at the given stop index.
   */
  void boardWithRegularTransfer(int stopIndex, int stopPos, int boardSlack);

  /**
   * Board the given trip(event) at the given stop index using constraint transfers
   * if it exists. If the boarding is not processed by the constrained transfers,
   * the implementation is also responsible for performing the fallback to board
   * from regular transfer.
   */
  void boardWithConstrainedTransfer(
    int stopIndex,
    int stopPos,
    int boardSlack,
    RaptorConstrainedBoardingSearch<T> txSearch
  );
}
