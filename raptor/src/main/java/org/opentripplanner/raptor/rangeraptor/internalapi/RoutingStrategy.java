package org.opentripplanner.raptor.rangeraptor.internalapi;

import java.util.Collections;
import java.util.Iterator;
import org.opentripplanner.raptor.api.model.RaptorAccessEgress;
import org.opentripplanner.raptor.api.model.RaptorOnBoardAccess;
import org.opentripplanner.raptor.api.model.RaptorTripSchedule;
import org.opentripplanner.raptor.api.view.ArrivalView;
import org.opentripplanner.raptor.rangeraptor.RangeRaptor;
import org.opentripplanner.raptor.spi.RaptorConstrainedBoardingSearch;
import org.opentripplanner.raptor.spi.RaptorRoute;
import org.opentripplanner.raptor.spi.RaptorTimeTable;

/**
 * Provides alternative implementations of some logic within the {@link RangeRaptor}.
 *
 * @param <T> The TripSchedule type defined by the user of the raptor API.
 */
public interface RoutingStrategy<T extends RaptorTripSchedule> {
  /**
   * Add access path to state. This should be called in the matching round and appropriate place in
   * the algorithm according to the {@link RaptorAccessEgress#numberOfRides()} and {@link
   * RaptorAccessEgress#stopReachedOnBoard()}.
   *
   * @param departureTime The access departure time. The current iteration departure time or
   *                      the time-shifted departure time for access with opening hours.
   */
  void setAccessToStop(RaptorAccessEgress accessPath, int departureTime);

  /**
   * Prepare the {@link RoutingStrategy} to route using the {@link RaptorTimeTable}.
   */
  void prepareForTransitWith(RaptorRoute<T> route);

  /**
   * First the {@link #prepareForTransitWith(RaptorRoute)} is called, then this method is
   * called before alight and board methods are called. This allows the strategy to update
   * the state before alighting and boarding is processed. Especially if there is a change
   * in the state as a result of passing through a stop this method might come in handy.
   */
  default void prepareForNextStop(int stopIndex, int stopPos) {}

  /**
   * Alight the current trip at the given stop.
   */
  void alightOnlyRegularTransferExist(
    final int stopIndex,
    final int stopPos,
    final int alightSlack
  );

  /**
   * Alight the current trip at the given stop with the arrival times.
   */
  void alightConstrainedTransferExist(
    final int stopIndex,
    final int stopPos,
    final int alightSlack
  );

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

  default void registerOnBoardAccessStopArrival(RaptorOnBoardAccess access, int boardTime) {
    throw new UnsupportedOperationException(
      "On-board access is not yet supported for this routing strategy"
    );
  }

  /**
   * @return an iterator over all on-board stop-arrivals currently in the state.
   * The iterator allows removal and this is the expected way to remove or a stop-arrival. When
   * removed is when we consider a stop-arrival to be "consumed".
   */
  default Iterator<? extends ArrivalView<T>> consumeOnBoardStopArrivals() {
    return Collections.emptyIterator();
  }

  /**
   * @return true if boarding successful, false otherwise
   */
  default boolean boardAsOnBoardAccess(
    ArrivalView<T> prevArrival,
    int stopPositionInPattern,
    T trip
  ) {
    throw new UnsupportedOperationException(
      "On-board access is not yet supported for this routing strategy"
    );
  }
}
