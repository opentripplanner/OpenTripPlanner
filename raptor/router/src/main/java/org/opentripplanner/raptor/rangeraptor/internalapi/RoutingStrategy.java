package org.opentripplanner.raptor.rangeraptor.internalapi;

import javax.annotation.Nullable;
import org.opentripplanner.raptor.api.model.RaptorAccessEgress;
import org.opentripplanner.raptor.api.model.RaptorStartOnBoardAccess;
import org.opentripplanner.raptor.api.view.ArrivalView;
import org.opentripplanner.raptor.rangeraptor.RangeRaptor;
import org.opentripplanner.raptor.spi.RaptorConstrainedBoardingSearch;
import org.opentripplanner.raptor.spi.RaptorRoute;
import org.opentripplanner.raptor.spi.RaptorTimeTable;
import org.opentripplanner.raptor.spi.RaptorTripSchedule;

/**
 * Provides alternative implementations of some logic within the {@link RangeRaptor}.
 *
 * @param <T> The TripSchedule type defined by the user of the raptor API.
 */
public interface RoutingStrategy<T extends RaptorTripSchedule> {
  /**
   * Add access path to state. This should be called in the matching round and appropriate place in
   * the algorithm according to the {@link RaptorAccessEgress#numberOfRides()} and {@link
   * RaptorAccessEgress#arrivedOnBoard()}.
   *
   * @param departureTime The access departure time. The current iteration departure time or
   *                      the time-shifted departure time for access with opening hours.
   */
  void addAccessStopArrival(RaptorAccessEgress accessPath, int departureTime);

  /**
   * Add a start-on-board access arrival to state. Called when the traveller is already on board a
   * vehicle at the start of the search.
   */
  default void addStartOnBoardAccessStopArrival(RaptorStartOnBoardAccess access, int boardTime) {
    throw createStartOnBoardAccessNotSupportedException();
  }

  /**
   * Prepare the {@link RoutingStrategy} to route using the {@link RaptorTimeTable}.
   */
  void prepareForTransitWith(RaptorRoute<T> route);

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

  /**
   * @return all on-board trip access arrivals for the given {@code routeIndex}. The arrivals are
   * removed from state and can only be fetched once. The method returns {@code null} if no
   * arrivals exist - this should be very efficient to check.
   */
  @Nullable
  default OnTripAccessArrivals<T> consumeStartOnBoardStopArrivalsForRoute(int routeIndex) {
    return null;
  }

  /**
   * Board the given {@code trip} at the given {@code stopPositionInPattern} using an
   * start-on-board access arrival as the previous state.
   */
  default void boardWithStartOnBoardAccess(
    ArrivalView<T> prevArrival,
    T trip,
    int stopPositionInPattern
  ) {
    throw createStartOnBoardAccessNotSupportedException();
  }

  private static RuntimeException createStartOnBoardAccessNotSupportedException() {
    return new UnsupportedOperationException(
      "On-board trip access is not yet supported for this routing strategy"
    );
  }
}
